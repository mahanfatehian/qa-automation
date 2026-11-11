package com.blueprintqa.runner.web;

import com.blueprintqa.runner.engine.SeleniumFactory;
import com.blueprintqa.runner.model.GraphNode;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class PickController {
    private static final String SCRIPT = """
            const view = { width: window.innerWidth, height: window.innerHeight };
            const seen = new Set();
            const elements = [];
            const nodes = document.body ? document.body.querySelectorAll('a,button,input,select,textarea,label,h1,h2,p,span,li,td,[data-testid],[role="button"]') : [];
            for (const el of nodes) {
              if (elements.length >= 40) break;
              const rect = el.getBoundingClientRect();
              if (rect.width < 4 || rect.height < 4) continue;
              if (rect.bottom < 0 || rect.top > view.height || rect.right < 0 || rect.left > view.width) continue;
              const style = getComputedStyle(el);
              if (style.visibility === 'hidden' || style.display === 'none') continue;
              const text = (el.innerText || el.value || el.getAttribute('aria-label') || '').trim().replace(/\\s+/g, ' ').slice(0, 80);
              const key = el.tagName + '|' + text + '|' + Math.round(rect.x) + '|' + Math.round(rect.y);
              if (seen.has(key)) continue;
              seen.add(key);
              const selectors = [];
              const testid = el.getAttribute('data-testid') || el.getAttribute('data-test');
              if (testid) selectors.push({ kind: 'CSS', value: '[data-testid="' + testid.replace(/"/g, '\\\\"') + '"]', rank: 1 });
              if (el.id && !/\\d{4,}/.test(el.id)) selectors.push({ kind: 'CSS', value: '#' + CSS.escape(el.id), rank: 2 });
              const name = el.getAttribute('name');
              if (name) selectors.push({ kind: 'CSS', value: el.tagName.toLowerCase() + '[name="' + name.replace(/"/g, '\\\\"') + '"]', rank: 3 });
              const aria = el.getAttribute('aria-label');
              if (aria) selectors.push({ kind: 'XPATH', value: '//' + el.tagName.toLowerCase() + '[@aria-label="' + aria.replace(/"/g, '') + '"]', rank: 4 });
              if (el.id) selectors.push({ kind: 'XPATH', value: '//' + el.tagName.toLowerCase() + '[@id="' + el.id.replace(/"/g, '') + '"]', rank: 5 });
              elements.push({
                tag: el.tagName.toLowerCase(),
                text,
                x: rect.x, y: rect.y, width: rect.width, height: rect.height,
                selectors
              });
            }
            return { viewport: view, elements };
            """;

    private final SeleniumFactory selenium;

    public PickController(SeleniumFactory selenium) {
        this.selenium = selenium;
    }

    @PostMapping("/internal/pick")
    public Map<String, Object> pick(@RequestBody Map<String, Object> body) {
        GraphNode start = new GraphNode();
        start.data = new LinkedHashMap<>();
        start.data.put("browser", text(body.get("browser"), "chrome"));
        start.data.put("headless", Boolean.TRUE.equals(body.get("headless")) || "true".equalsIgnoreCase(String.valueOf(body.get("headless"))));
        start.data.put("windowWidth", 1280);
        start.data.put("windowHeight", 800);
        WebDriver driver = null;
        try {
            String url = text(body.get("url"), "");
            if (url.isBlank()) {
                throw new IllegalArgumentException("Pick needs the Open page URL");
            }
            driver = selenium.open(start, "pick", 2);
            driver.navigate().to(url);
            Object found = ((JavascriptExecutor) driver).executeScript(SCRIPT);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("currentUrl", driver.getCurrentUrl());
            result.put("title", driver.getTitle());
            result.put("screenshotBase64", ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64));
            result.put("page", found);
            result.put("error", null);
            return result;
        } catch (Exception ex) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", false);
            result.put("page", Map.of("elements", java.util.List.of()));
            result.put("error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            result.put("screenshotBase64", "");
            return result;
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ignored) {
                    // already closed
                }
            }
        }
    }

    private String text(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
