package com.blueprintqa.runner.web;

import com.blueprintqa.runner.engine.ActiveSessions;
import com.blueprintqa.runner.engine.DagExecutorService;
import com.blueprintqa.runner.engine.Pointer;
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
            const named = (n) => {
              if (!n || !n.tagName) return false;
              const tag = n.tagName.toLowerCase();
              if (tag.includes('-')) return true;
              return !!(n.id || n.getAttribute('name') || n.getAttribute('data-testid') || n.getAttribute('data-test') || n.getAttribute('data-cro-id') || n.getAttribute('aria-label'));
            };
            let pierce = 0;
            while (node.shadowRoot && pierce++ < 4) {
              const inner = node.shadowRoot.elementFromPoint(x, y);
              if (!inner || inner === node) break;
              if (!named(inner) && named(node)) break;
              node = inner;
            }
            const childAt = (parent) => {
              if (!parent || !parent.children) return null;
              const kids = [...parent.children].filter((child) => {
                const box = child.getBoundingClientRect();
                return box.width > 4 && box.height > 4 && x >= box.left && x <= box.right && y >= box.top && y <= box.bottom;
              });
              const namedKids = kids.filter(named);
              return namedKids.length === 1 ? namedKids[0] : null;
            };
            let down = 0;
            while (down++ < 3) {
              const inner = childAt(node);
              if (!inner || inner === node) break;
              node = inner;
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
            const acc = (node.getAttribute('aria-label') || node.getAttribute('title') || node.getAttribute('alt') || node.getAttribute('name') || '').trim();
            const explicitRole = (node.getAttribute('role') || '').toLowerCase();
            const implied = explicitRole || (tag === 'a' ? 'link' : tag === 'button' ? 'button' : tag === 'img' ? 'img' : '');
            if (acc) selectors.push({ kind: 'ROLE', value: (implied || '*') + '|' + acc.split('|').join(' ') });
            const testid = node.getAttribute('data-testid') || node.getAttribute('data-test');
            const cro = node.getAttribute('data-cro-id') || '';
            if (testid) selectors.push({ kind: 'CSS', value: '[data-testid="' + testid.replace(/"/g, '') + '"]' });
            const elName = node.getAttribute('name') || '';
            if (elName) selectors.push({ kind: 'CSS', value: tag + '[name="' + elName.replace(/"/g, '') + '"]' });
            if (cro) selectors.push({ kind: 'CSS', value: '[data-cro-id="' + cro.replace(/"/g, '') + '"]' });
            if (node.id) selectors.push({ kind: 'ID', value: node.id });
            const name = node.getAttribute('name');
            if (name) selectors.push({ kind: 'NAME', value: name });
            if (node.id) selectors.push({ kind: 'CSS', value: '#' + CSS.escape(node.id) });
            const text = (node.innerText || node.value || node.getAttribute('aria-label') || '').trim().replace(/\\s+/g, ' ').slice(0, 120);
            const suggested = [];
            if (kind === 'select') suggested.push('SELECT_OPTION');
            if (kind === 'checkbox' || kind === 'radio') suggested.push('SET_CHECKBOX');
            if (kind === 'text-input' || kind === 'password' || kind === 'textarea') suggested.push('TYPE_TEXT');
            if (kind === 'button' || kind === 'link' || elName || testid || cro || tag.includes('-')) suggested.push('CLICK');
            if (kind === 'file') suggested.push('UPLOAD');
            suggested.push('VERIFY', 'SEARCH_ELEMENT');
            const path = [];
            let cursor = node;
            let depth = 0;
            while (cursor && cursor.tagName && cursor.tagName !== 'HTML' && depth++ < 12) {
              const root = cursor.getRootNode && cursor.getRootNode();
              const parent = cursor.parentElement || (root && root.host) || null;
              let nth = 1;
              if (parent) {
                const kids = parent.shadowRoot && !cursor.parentElement ? [...parent.shadowRoot.children] : [...parent.children];
                nth = Math.max(1, kids.filter((child) => child.tagName === cursor.tagName).indexOf(cursor) + 1);
              }
              path.unshift({
                tag: cursor.tagName.toLowerCase(),
                nth,
                id: cursor.id || '',
                name: cursor.getAttribute('name') || '',
                testid: cursor.getAttribute('data-testid') || cursor.getAttribute('data-test') || cursor.getAttribute('data-cy') || cursor.getAttribute('data-qa') || '',
                cro: cursor.getAttribute('data-cro-id') || '',
                type: (cursor.getAttribute('type') || '').toLowerCase(),
                aria: cursor.getAttribute('aria-label') || '',
                placeholder: cursor.getAttribute('placeholder') || '',
                role: cursor.getAttribute('role') || ''
              });
              cursor = parent;
            }
            return {
              tag, type, kind, text, name: name || '', id: node.id || '',
              placeholder: node.getAttribute('placeholder') || '',
              aria: node.getAttribute('aria-label') || '',
              role: implied || '',
              x: rect.x, y: rect.y, width: rect.width, height: rect.height,
              options, selectors, suggested, path
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
            held.put(id, new Held(driver, System.currentTimeMillis(), ""));
            Map<String, Object> result = shot(driver);
            result.put("ok", true);
            result.put("sessionId", id);
            result.put("error", null);
            return result;
        } catch (Exception ex) {
            executor.parkInspect(driver);
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

    @PostMapping("/internal/inspect/{id}/source")
    public Map<String, Object> source(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        Held session = fresh(id);
        if (session == null) {
            return Map.of("ok", false, "error", "Selection session expired");
        }
        boolean refresh = body != null && Boolean.TRUE.equals(body.get("refresh"));
        if (refresh || session.pageSource == null || session.pageSource.isBlank()) {
            try {
                session.pageSource = session.driver.getPageSource();
            } catch (Exception ex) {
                return Map.of("ok", false, "error", ex.getMessage() == null ? "Could not cache the page" : ex.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("html", session.pageSource);
        return result;
    }

    @PostMapping("/internal/inspect/{id}/close")
    public Map<String, Object> close(@PathVariable String id) {
        Held session = held.remove(id);
        if (session != null) {
            executor.parkInspect(session.driver);
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
            executor.discardInspect(entry.getValue().driver);
            return true;
        });
    }

    private void closeAll() {
        held.values().forEach(session -> executor.parkInspect(session.driver));
        held.clear();
    }

    private Map<String, Object> shot(WebDriver driver) {
        Pointer.settleForShot(driver);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentUrl", driver.getCurrentUrl());
        result.put("title", driver.getTitle());
        String jpeg = jpeg(driver);
        result.put("screenshotBase64", jpeg != null ? jpeg : ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64));
        result.put("screenshotMime", jpeg != null ? "image/jpeg" : "image/png");
        Object size = ((JavascriptExecutor) driver).executeScript("return {width: window.innerWidth, height: window.innerHeight}");
        result.put("viewport", size);
        return result;
    }

    private String jpeg(WebDriver driver) {
        try {
            WebDriver augmented = new org.openqa.selenium.remote.Augmenter().augment(driver);
            if (augmented instanceof org.openqa.selenium.chromium.HasCdp cdp) {
                Object raw = cdp.executeCdpCommand("Page.captureScreenshot", Map.of("format", "jpeg", "quality", 55));
                if (raw instanceof Map<?, ?> map && map.get("data") != null) {
                    return String.valueOf(map.get("data"));
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
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
        private String pageSource;

        private Held(WebDriver driver, long touched, String pageSource) {
            this.driver = driver;
            this.touched = touched;
            this.pageSource = pageSource;
        }
    }
}
