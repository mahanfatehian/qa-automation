package com.blueprintqa.runner.web;

import com.blueprintqa.runner.engine.ActiveSessions;
import com.blueprintqa.runner.engine.DagExecutorService;
import com.blueprintqa.runner.model.GraphEdge;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.WorkflowJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class InspectController {
    private static final long TTL_MS = 120_000L;
    private static final String DESCRIBE = """
            const x = arguments[0], y = arguments[1];
            const stack = document.elementsFromPoint ? document.elementsFromPoint(x, y) : [document.elementFromPoint(x, y)];
            let node = null;
            let bestArea = Infinity;
            for (const candidate of stack) {
              if (!candidate || !candidate.tagName) continue;
              const tag = candidate.tagName;
              if (tag === 'HTML' || tag === 'BODY') continue;
              const box = candidate.getBoundingClientRect();
              const area = box.width * box.height;
              if (area > 8 && area < bestArea) { node = candidate; bestArea = area; }
            }
            if (!node) return null;
            const interesting = (n) => {
              const tag = n.tagName.toLowerCase();
              const type = (n.getAttribute('type') || '').toLowerCase();
              return ['a','button','input','select','textarea','label'].includes(tag) || n.getAttribute('role') === 'button' || type === 'checkbox' || type === 'radio';
            };
            for (let i = 0; i < 3 && node.parentElement && !interesting(node); i++) {
              const parent = node.parentElement;
              if (!parent.tagName || parent.tagName === 'HTML' || parent.tagName === 'BODY') break;
              node = parent;
            }
            const tag = node.tagName.toLowerCase();
            const type = (node.getAttribute('type') || '').toLowerCase();
            let kind = 'text';
            if (tag === 'select') kind = 'select';
            else if (tag === 'textarea') kind = 'textarea';
            else if (tag === 'button' || type === 'submit' || type === 'button' || node.getAttribute('role') === 'button') kind = 'button';
            else if (tag === 'a') kind = 'link';
            else if (type === 'checkbox') kind = 'checkbox';
            else if (type === 'radio') kind = 'radio';
            else if (type === 'file') kind = 'file';
            else if (tag === 'input') kind = type === 'password' ? 'password' : 'text-input';
            else if (tag === 'img') kind = 'image';
            const rect = node.getBoundingClientRect();
            const options = [];
            if (tag === 'select') {
              for (const opt of node.options) options.push({ text: opt.text, value: opt.value });
            }
            const selectors = [];
            const testid = node.getAttribute('data-testid') || node.getAttribute('data-test');
            if (testid) selectors.push({ kind: 'CSS', value: '[data-testid="' + testid.replace(/"/g, '') + '"]' });
            if (node.id) selectors.push({ kind: 'ID', value: node.id });
            const name = node.getAttribute('name');
            if (name) selectors.push({ kind: 'NAME', value: name });
            if (node.id) selectors.push({ kind: 'CSS', value: '#' + CSS.escape(node.id) });
            const text = (node.innerText || node.value || node.getAttribute('aria-label') || '').trim().replace(/\\s+/g, ' ').slice(0, 120);
            const suggested = [];
            if (kind === 'select') suggested.push('SELECT_OPTION');
            if (kind === 'checkbox' || kind === 'radio') suggested.push('SET_CHECKBOX');
            if (kind === 'text-input' || kind === 'password' || kind === 'textarea') suggested.push('TYPE_TEXT');
            if (kind === 'button' || kind === 'link') suggested.push('CLICK');
            if (kind === 'file') suggested.push('UPLOAD');
            suggested.push('VERIFY', 'SEARCH_ELEMENT');
            return {
              tag, type, kind, text, name: name || '', id: node.id || '',
              placeholder: node.getAttribute('placeholder') || '',
              aria: node.getAttribute('aria-label') || '',
              x: rect.x, y: rect.y, width: rect.width, height: rect.height,
              options, selectors, suggested
            };
            """;

    private final DagExecutorService executor;
    private final ActiveSessions activeSessions;
    private final ObjectMapper mapper;
    private final Map<String, Held> held = new ConcurrentHashMap<>();

    public InspectController(DagExecutorService executor, ActiveSessions activeSessions, ObjectMapper mapper) {
        this.executor = executor;
        this.activeSessions = activeSessions;
        this.mapper = mapper;
    }

    @PostMapping("/internal/inspect/start")
    public Map<String, Object> start(@RequestBody Map<String, Object> body) {
        expire();
        closeAll();
        WebDriver driver = null;
        try {
            WorkflowJob job = new WorkflowJob();
            job.nodes = mapper.convertValue(body.get("nodes"), mapper.getTypeFactory().constructCollectionType(List.class, GraphNode.class));
            job.edges = mapper.convertValue(body.get("edges"), mapper.getTypeFactory().constructCollectionType(List.class, GraphEdge.class));
            if (body.get("variables") instanceof Map<?, ?> variables) {
                job.variables = new LinkedHashMap<>();
                variables.forEach((key, value) -> job.variables.put(String.valueOf(key), value));
            }
            job.maxSteps = 80;
            driver = executor.openContext(job, text(body.get("stopBefore")));
            String id = UUID.randomUUID().toString();
            held.put(id, new Held(driver, System.currentTimeMillis()));
            Map<String, Object> result = shot(driver);
            result.put("ok", true);
            result.put("sessionId", id);
            result.put("error", null);
            return result;
        } catch (Exception ex) {
            if (driver != null) {
                activeSessions.remove(driver);
                try {
                    driver.quit();
                } catch (Exception ignored) {
                    // already closed
                }
            }
            return Map.of("ok", false, "error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }

    @PostMapping("/internal/inspect/{id}/hover")
    public Map<String, Object> hover(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Held session = fresh(id);
        if (session == null) {
            return Map.of("ok", false, "error", "Selection session expired");
        }
        int x = number(body.get("x"));
        int y = number(body.get("y"));
        Object described = ((JavascriptExecutor) session.driver).executeScript(DESCRIBE, x, y);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("element", described);
        return result;
    }

    @PostMapping("/internal/inspect/{id}/close")
    public Map<String, Object> close(@PathVariable String id) {
        Held session = held.remove(id);
        if (session != null) {
            activeSessions.remove(session.driver);
            try {
                session.driver.quit();
            } catch (Exception ignored) {
                // already closed
            }
        }
        return Map.of("ok", true);
    }

    private Held fresh(String id) {
        expire();
        Held session = held.get(id);
        if (session == null) {
            return null;
        }
        session.touched = System.currentTimeMillis();
        return session;
    }

    private void expire() {
        long now = System.currentTimeMillis();
        held.entrySet().removeIf(entry -> {
            if (now - entry.getValue().touched < TTL_MS) {
                return false;
            }
            activeSessions.remove(entry.getValue().driver);
            try {
                entry.getValue().driver.quit();
            } catch (Exception ignored) {
                // already closed
            }
            return true;
        });
    }

    private void closeAll() {
        held.values().forEach(session -> {
            activeSessions.remove(session.driver);
            try {
                session.driver.quit();
            } catch (Exception ignored) {
                // already closed
            }
        });
        held.clear();
    }

    private Map<String, Object> shot(WebDriver driver) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentUrl", driver.getCurrentUrl());
        result.put("title", driver.getTitle());
        result.put("screenshotBase64", ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64));
        Object size = ((JavascriptExecutor) driver).executeScript("return {width: window.innerWidth, height: window.innerHeight}");
        result.put("viewport", size);
        return result;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static final class Held {
        private final WebDriver driver;
        private long touched;

        private Held(WebDriver driver, long touched) {
            this.driver = driver;
            this.touched = touched;
        }
    }
}
