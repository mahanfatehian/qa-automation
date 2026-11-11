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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DagExecutorService {
    private static final Logger log = LoggerFactory.getLogger(DagExecutorService.class);
    private static final java.util.Set<String> INSPECT_SKIP = java.util.Set.of(
            "SCREENSHOT", "ASSERT", "VERIFY", "READ_URL", "READ_TITLE", "COUNT_ELEMENTS", "EXTRACT_CONTENT", "IF_CONDITION"
    );
    private final ObjectMapper mapper;
    private final NodeRegistry registry;
    private final SeleniumFactory selenium;
    private final TelemetryPublisher telemetry;
    private final ActiveSessions sessions;
    private final GridBudget budget;
    private final InspectBrowserPool inspectBrowsers;

    public DagExecutorService(ObjectMapper mapper, NodeRegistry registry, SeleniumFactory selenium, TelemetryPublisher telemetry, ActiveSessions sessions, GridBudget budget, InspectBrowserPool inspectBrowsers) {
        this.mapper = mapper;
        this.registry = registry;
        this.selenium = selenium;
        this.telemetry = telemetry;
        this.sessions = sessions;
        this.budget = budget;
        this.inspectBrowsers = inspectBrowsers;
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
        budget.acquire();
        try {
            telemetry.publish(job, "RUN_STARTED", "", "", 0, "Blueprint started", "", Map.of(), "");
            GraphNode start = job.start();
            driver = selenium.open(start, job.workflowName);
            sessions.add(driver);
            ExecutionContext context = new ExecutionContext(driver, job.variables);
            runNode(start, job, context, new AtomicInteger(0));
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
            budget.release();
        }
    }

    public WebDriver openContext(WorkflowJob job, String stopBefore) {
        GraphNode start = job.start();
        WebDriver driver = inspectBrowsers.take(start);
        sessions.add(driver);
        driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(12));
        driver.manage().timeouts().scriptTimeout(java.time.Duration.ofSeconds(8));
        ElementFinder.INSPECT_CAP.set(4);
        try {
            ExecutionContext context = new ExecutionContext(driver, job.variables);
            context.setVariable("__inspect", Boolean.TRUE);
            for (GraphNode node : InspectPath.prefix(job, stopBefore)) {
                replayFast(node, job, context);
            }
        } finally {
            ElementFinder.INSPECT_CAP.remove();
        }
        if (blank(driver)) {
            String url = firstUrl(job);
            if (!url.isBlank()) {
                driver.navigate().to(url);
            }
        }
        return driver;
    }

    public void parkInspect(WebDriver driver) {
        if (driver == null) {
            return;
        }
        sessions.remove(driver);
        inspectBrowsers.park(driver);
    }

    public void discardInspect(WebDriver driver) {
        if (driver == null) {
            return;
        }
        sessions.remove(driver);
        inspectBrowsers.discard(driver);
    }

    private void replayFast(GraphNode node, WorkflowJob job, ExecutionContext context) {
        if (node == null || INSPECT_SKIP.contains(node.semanticType())) {
            return;
        }
        resolveInputs(node, job, context);
        if ("LOOP_ELEMENTS".equals(node.semanticType())) {
            String xpath = context.interpolate(node.str("xpath", ""));
            if (xpath.isBlank()) {
                return;
            }
            java.util.List<org.openqa.selenium.WebElement> found = context.driver().findElements(org.openqa.selenium.By.xpath(xpath));
            if (!found.isEmpty()) {
                context.setPin(node.id, "current_element", found.get(0));
                context.setPin(node.id, "current_element_ref", new ElementRef("XPATH", "(" + xpath + ")[1]"));
            }
            return;
        }
        try {
            NodeOutput output = registry.get(node.semanticType()).execute(node, context);
            store(node, output, context);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not reach this step: " + node.str("title", node.semanticType()) + " — " + ex.getMessage(), ex);
        }
    }

    private boolean blank(WebDriver driver) {
        try {
            String current = driver.getCurrentUrl();
            return current == null || current.isBlank() || current.startsWith("data:") || current.startsWith("about:");
        } catch (Exception ex) {
            return true;
        }
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
            GraphNode next = replayTarget(job, node.id, handle, stopBefore);
            if (next != null) {
                replayNode(next, job, context, stopBefore, steps);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not reach the page state: " + ex.getMessage(), ex);
        }
    }

    private void runNode(GraphNode node, WorkflowJob job, ExecutionContext context, AtomicInteger steps) {
        int guard = job.maxSteps <= 0 ? 500 : job.maxSteps;
        if (steps.incrementAndGet() > guard) {
            throw new IllegalStateException("Stopped: this blueprint hit the " + guard + " step guard");
        }
        resolveInputs(node, job, context);
        borrowSelector(node, job);
        long started = System.nanoTime();
        telemetry.publish(job, "NODE_RUNNING", node.id, node.semanticType(), 0, "", "", Map.of(), "");
        NodeOutput output;
        try {
            if ("LOOP_ELEMENTS".equals(node.semanticType())) {
                runLoop(node, job, context, steps, started);
                return;
            }
            output = registry.get(node.semanticType()).execute(node, context);
        } catch (Exception ex) {
            telemetry.publish(job, "NODE_FAILED", node.id, node.semanticType(), elapsed(started), branched(context, friendly(ex)), stack(ex), stepDetail(node, context, Map.of()), screenshot(context.driver()));
            throw new IllegalStateException(where(context) + friendly(ex), ex);
        }
        store(node, output, context);
        String handle = output.nextExec == null || output.nextExec.isBlank() ? "exec_out" : output.nextExec;
        List<GraphNode> next = successors(job, node.id, handle);
        String note = output.message == null ? "" : output.message;
        if (next.size() > 1) {
            note = (note.isBlank() ? "" : note + " · ") + "forked " + next.size() + " browsers";
        }
        telemetry.publish(job, "NODE_PASSED", node.id, node.semanticType(), elapsed(started), branched(context, note), "", stepDetail(node, context, output.pins), screenshot(context.driver()));
        dispatch(next, job, context, steps);
    }

    private void runLoop(GraphNode node, WorkflowJob job, ExecutionContext context, AtomicInteger steps, long started) {
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
            context.setPin(node.id, "current_element_ref", new ElementRef("XPATH", "(" + xpath + ")[" + (index + 1) + "]"));
            context.setVariable("loop_index", index);
            context.setVariable("loop_count", count);
            dispatch(body, job, context, steps);
        }
        dispatch(successors(job, node.id, "exec_completed"), job, context, steps);
    }

    private void dispatch(List<GraphNode> next, WorkflowJob job, ExecutionContext context, AtomicInteger steps) {
        if (next == null || next.isEmpty()) {
            return;
        }
        if (next.size() == 1) {
            runNode(next.get(0), job, context, steps);
            return;
        }
        fork(next, job, context, steps);
    }

    private void fork(List<GraphNode> next, WorkflowJob job, ExecutionContext context, AtomicInteger steps) {
        BrowserCheckpoint checkpoint = BrowserCheckpoint.capture(context.driver());
        ExecutionContext origin = context.fork(context.driver(), context.branch());
        int copiedSteps = steps.get();
        List<Thread> parallel = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();
        for (BranchPlan.Entry entry : BranchPlan.of(next.size())) {
            if (!entry.opensNewSession()) {
                if (entry.index() != 0 || entry.sharesDriver()) {
                    throw new IllegalStateException("Only the first execution pin keeps the live session");
                }
                continue;
            }
            int branch = entry.index() + 1;
            GraphNode target = next.get(entry.index());
            Thread thread = Thread.ofVirtual().name("branch-" + job.runId + "-" + branch).start(() -> {
                WebDriver clone = null;
                boolean held = false;
                try {
                    budget.acquire();
                    held = true;
                    clone = selenium.open(job.start(), sessionName(job, branch), 4);
                    sessions.add(clone);
                    checkpoint.restore(clone);
                    runNode(target, job, origin.fork(clone, branchName(context, branch)), new AtomicInteger(copiedSteps));
                } catch (Exception ex) {
                    synchronized (failures) {
                        failures.add(ex);
                    }
                } finally {
                    if (clone != null) {
                        sessions.remove(clone);
                        try {
                            clone.quit();
                        } catch (Exception ignored) {
                            log.debug("branch browser already closed");
                        }
                    }
                    if (held) {
                        budget.release();
                    }
                }
            });
            parallel.add(thread);
        }
        try {
            runNode(next.get(0), job, origin.fork(context.driver(), branchName(context, 1)), new AtomicInteger(copiedSteps));
        } catch (Exception ex) {
            synchronized (failures) {
                failures.add(ex);
            }
        }
        for (Thread thread : parallel) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                synchronized (failures) {
                    failures.add(ex);
                }
            }
        }
        if (!failures.isEmpty()) {
            Throwable first = failures.get(0);
            if (first instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(first.getMessage(), first);
        }
    }

    private String branchName(ExecutionContext context, int branch) {
        if (context.branch() == null || context.branch().isBlank()) {
            return "branch " + branch;
        }
        return context.branch() + "." + branch;
    }

    private String sessionName(WorkflowJob job, int branch) {
        String name = job.workflowName == null || job.workflowName.isBlank() ? "blueprint" : job.workflowName;
        return name + " · branch " + branch;
    }

    private String branched(ExecutionContext context, String message) {
        String text = message == null ? "" : message;
        if (context.branch() == null || context.branch().isBlank()) {
            return text;
        }
        return text.isBlank() ? context.branch() : context.branch() + " · " + text;
    }

    private GraphNode replayTarget(WorkflowJob job, String nodeId, String handle, String stopBefore) {
        List<GraphNode> next = successors(job, nodeId, handle);
        if (next.isEmpty()) {
            return null;
        }
        if (stopBefore == null || stopBefore.isBlank()) {
            return next.get(0);
        }
        for (GraphNode candidate : next) {
            if (reaches(job, candidate, stopBefore, new HashSet<>())) {
                return candidate;
            }
        }
        return next.get(0);
    }

    private boolean reaches(WorkflowJob job, GraphNode node, String stopBefore, Set<String> seen) {
        if (node == null || stopBefore == null || !seen.add(node.id)) {
            return false;
        }
        if (stopBefore.equals(node.id)) {
            return true;
        }
        if (job.edges == null) {
            return false;
        }
        for (GraphEdge edge : job.edges) {
            if (edge == null || !node.id.equals(edge.source) || !edge.sourceHandleOrDefault().startsWith("exec_")) {
                continue;
            }
            if (reaches(job, job.node(edge.target), stopBefore, seen)) {
                return true;
            }
        }
        return false;
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
            Object ref = context.getPin(edge.source, edge.sourceHandleOrDefault() + "_ref");
            if (ref != null) {
                context.putInput(target + "_ref", ref);
            }
        }
    }

    private void borrowSelector(GraphNode node, WorkflowJob job) {
        if (node == null || node.data == null || !"SEARCH_ELEMENT".equals(node.semanticType()) || hasSelector(node)) {
            return;
        }
        for (GraphNode next : successors(job, node.id, "exec_out")) {
            if (!hasSelector(next)) {
                continue;
            }
            if (next.data.get("selectors") != null) {
                node.data.put("selectors", next.data.get("selectors"));
            }
            String strategy = next.str("strategy", "");
            String query = next.str("query", next.str("xpath", ""));
            if (!strategy.isBlank()) {
                node.data.put("strategy", strategy);
            }
            if (!query.isBlank()) {
                node.data.put("query", query);
                node.data.put("xpath", query);
            }
            return;
        }
    }

    private boolean hasSelector(GraphNode node) {
        if (node == null || node.data == null) {
            return false;
        }
        Object raw = node.data.get("selectors");
        if (raw instanceof java.util.List<?> list && !list.isEmpty()) {
            return true;
        }
        return !node.str("query", "").isBlank() || !node.str("xpath", "").isBlank();
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

    private Map<String, Object> stepDetail(GraphNode node, ExecutionContext context, Map<String, Object> extra) {
        Map<String, Object> detail = new java.util.LinkedHashMap<>();
        if (extra != null) {
            detail.putAll(telemetry.safe(extra));
        }
        String url = "";
        try {
            url = context.driver().getCurrentUrl();
        } catch (Exception ignored) {
            url = "";
        }
        if (url != null && !url.isBlank()) {
            detail.put("url", url);
        }
        String selector = node.str("query", node.str("xpath", ""));
        if (!selector.isBlank()) {
            detail.put("selector", selector);
        }
        if (context.branch() != null && !context.branch().isBlank()) {
            detail.put("branch", context.branch());
        }
        return detail;
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
