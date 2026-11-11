package com.blueprintqa.runner.engine;

import com.blueprintqa.runner.model.GraphEdge;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import com.blueprintqa.runner.model.WorkflowJob;
import com.blueprintqa.runner.nodes.NodeRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DagExecutorService {
    private static final Logger log = LoggerFactory.getLogger(DagExecutorService.class);
    private final ObjectMapper mapper;
    private final NodeRegistry registry;
    private final SeleniumFactory selenium;
    private final TelemetryPublisher telemetry;
    private final ActiveSessions sessions;

    public DagExecutorService(ObjectMapper mapper, NodeRegistry registry, SeleniumFactory selenium, TelemetryPublisher telemetry, ActiveSessions sessions) {
        this.mapper = mapper;
        this.registry = registry;
        this.selenium = selenium;
        this.telemetry = telemetry;
        this.sessions = sessions;
    }

    public void executeJob(String json) {
        WorkflowJob job;
        try {
            job = mapper.readValue(json, WorkflowJob.class);
        } catch (Exception ex) {
            log.error("rejected unreadable blueprint job", ex);
            return;
        }
        WebDriver driver = null;
        try {
            telemetry.publish(job, "RUN_STARTED", "", "", 0, "Blueprint started", "", Map.of(), "");
            GraphNode start = job.start();
            driver = selenium.open(start, job.workflowName);
            sessions.add(driver);
            ExecutionContext context = new ExecutionContext(driver, job.variables);
            runNode(start, job, context, new int[]{0});
            telemetry.publish(job, "RUN_COMPLETED", "", "", 0, "Blueprint passed", "", Map.of(), screenshot(driver));
        } catch (Exception ex) {
            log.warn("blueprint {} failed: {}", job.runId, ex.getMessage());
            telemetry.publish(job, "RUN_FAILED", "", "", 0, friendly(ex), stack(ex), Map.of(), screenshot(driver));
        } finally {
            if (driver != null) {
                sessions.remove(driver);
                try {
                    driver.quit();
                } catch (Exception ignored) {
                    log.debug("driver already closed");
                }
            }
        }
    }

    public WebDriver openContext(WorkflowJob job, String stopBefore) {
        GraphNode start = job.start();
        WebDriver driver = selenium.open(start, "inspect", 1);
        sessions.add(driver);
        driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(20));
        try {
            replayNode(start, job, new ExecutionContext(driver, job.variables), stopBefore, new int[]{0});
        } catch (Exception ex) {
            if (driver.getCurrentUrl() == null || driver.getCurrentUrl().startsWith("data:")) {
                throw ex;
            }
        }
        String current = driver.getCurrentUrl();
        if (current == null || current.isBlank() || current.startsWith("data:")) {
            String url = firstUrl(job);
            if (!url.isBlank()) {
                driver.navigate().to(url);
            }
        }
        return driver;
    }

    private String firstUrl(WorkflowJob job) {
        if (job.nodes == null) {
            return "";
        }
        for (GraphNode node : job.nodes) {
            if (node != null && "NAVIGATE".equals(node.semanticType())) {
                String url = node.str("url", "");
                if (!url.isBlank()) {
                    return url;
                }
            }
        }
        return "";
    }

    private void replayNode(GraphNode node, WorkflowJob job, ExecutionContext context, String stopBefore, int[] steps) {
        if (node == null || (stopBefore != null && stopBefore.equals(node.id)) || steps[0]++ >= 80) {
            return;
        }
        resolveInputs(node, job, context);
        try {
            if ("LOOP_ELEMENTS".equals(node.semanticType())) {
                return;
            }
            NodeOutput output = registry.get(node.semanticType()).execute(node, context);
            store(node, output, context);
            String handle = output.nextExec == null || output.nextExec.isBlank() ? "exec_out" : output.nextExec;
            for (GraphNode next : successors(job, node.id, handle)) {
                replayNode(next, job, context, stopBefore, steps);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not reach the page state: " + ex.getMessage(), ex);
        }
    }

    private void runNode(GraphNode node, WorkflowJob job, ExecutionContext context, int[] steps) {
        int guard = job.maxSteps <= 0 ? 500 : job.maxSteps;
        if (steps[0]++ >= guard) {
            throw new IllegalStateException("Stopped: this blueprint hit the " + guard + " step guard");
        }
        resolveInputs(node, job, context);
        long started = System.nanoTime();
        telemetry.publish(job, "NODE_RUNNING", node.id, node.semanticType(), 0, "", "", Map.of(), "");
        try {
            if ("LOOP_ELEMENTS".equals(node.semanticType())) {
                runLoop(node, job, context, steps, started);
                return;
            }
            NodeOutput output = registry.get(node.semanticType()).execute(node, context);
            store(node, output, context);
            telemetry.publish(job, "NODE_PASSED", node.id, node.semanticType(), elapsed(started), output.message, "", telemetry.safe(output.pins), screenshot(context.driver()));
            String handle = output.nextExec == null || output.nextExec.isBlank() ? "exec_out" : output.nextExec;
            for (GraphNode next : successors(job, node.id, handle)) {
                runNode(next, job, context, steps);
            }
        } catch (Exception ex) {
            telemetry.publish(job, "NODE_FAILED", node.id, node.semanticType(), elapsed(started), friendly(ex), stack(ex), Map.of(), screenshot(context.driver()));
            throw new IllegalStateException(where(context) + friendly(ex), ex);
        }
    }

    private void runLoop(GraphNode node, WorkflowJob job, ExecutionContext context, int[] steps, long started) {
        String xpath = context.interpolate(node.str("xpath", ""));
        if (xpath == null || xpath.isBlank()) {
            throw new IllegalArgumentException("Repeat for each needs an XPath");
        }
        int count = context.driver().findElements(By.xpath(xpath)).size();
        NodeOutput summary = NodeOutput.pass("Matched " + count + " elements");
        summary.pins.put("count", count);
        store(node, summary, context);
        telemetry.publish(job, "NODE_PASSED", node.id, node.semanticType(), elapsed(started), summary.message, "", telemetry.safe(summary.pins), screenshot(context.driver()));
        List<GraphNode> body = successors(job, node.id, "exec_loop_body");
        for (int index = 0; index < count; index++) {
            List<WebElement> fresh = context.driver().findElements(By.xpath(xpath));
            if (index >= fresh.size()) {
                break;
            }
            context.setPin(node.id, "current_element", fresh.get(index));
            context.setVariable("loop_index", index);
            context.setVariable("loop_count", count);
            for (GraphNode child : body) {
                runNode(child, job, context, steps);
            }
        }
        for (GraphNode next : successors(job, node.id, "exec_completed")) {
            runNode(next, job, context, steps);
        }
    }

    private void resolveInputs(GraphNode node, WorkflowJob job, ExecutionContext context) {
        context.clearInputs();
        if (job.edges == null) {
            return;
        }
        for (GraphEdge edge : job.edges) {
            if (edge == null || !node.id.equals(edge.target)) {
                continue;
            }
            String target = edge.targetHandleOrDefault();
            if (target.startsWith("exec_")) {
                continue;
            }
            Object value = context.getPin(edge.source, edge.sourceHandleOrDefault());
            context.putInput(target, value);
        }
    }

    private void store(GraphNode node, NodeOutput output, ExecutionContext context) {
        if (output == null || output.pins == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : output.pins.entrySet()) {
            context.setPin(node.id, entry.getKey(), entry.getValue());
        }
        String saveAs = node.str("saveAsVariable", "");
        if (saveAs.isBlank()) {
            return;
        }
        Object value = output.pins.get("text_out");
        if (value == null) {
            value = output.pins.get("extracted_value");
        }
        if (value == null) {
            value = output.pins.get("bool_out");
        }
        context.setVariable(saveAs, value);
    }

    private List<GraphNode> successors(WorkflowJob job, String nodeId, String handle) {
        List<GraphNode> next = new ArrayList<>();
        if (job.edges == null) {
            return next;
        }
        for (GraphEdge edge : job.edges) {
            if (edge != null && nodeId.equals(edge.source) && handle.equals(edge.sourceHandleOrDefault())) {
                GraphNode target = job.node(edge.target);
                if (target != null) {
                    next.add(target);
                }
            }
        }
        return next;
    }

    private long elapsed(long started) {
        return Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
    }

    private String screenshot(WebDriver driver) {
        if (!(driver instanceof TakesScreenshot shots)) {
            return "";
        }
        try {
            return shots.getScreenshotAs(OutputType.BASE64);
        } catch (Exception ex) {
            return "";
        }
    }

    private String where(ExecutionContext context) {
        try {
            String url = context.driver().getCurrentUrl();
            return url == null || url.isBlank() ? "" : url + " — ";
        } catch (Exception ex) {
            return "";
        }
    }

    private String friendly(Exception ex) {
        Throwable cursor = ex;
        while (cursor.getCause() != null && cursor.getCause() != cursor && (cursor.getMessage() == null || cursor.getMessage().isBlank())) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        if (message == null || message.isBlank()) {
            return cursor.getClass().getSimpleName();
        }
        return message.length() > 1500 ? message.substring(0, 1500) : message;
    }

    private String stack(Exception ex) {
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        String text = writer.toString();
        return text.length() > 4000 ? text.substring(0, 4000) : text;
    }
}
