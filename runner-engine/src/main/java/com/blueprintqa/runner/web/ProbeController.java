package com.blueprintqa.runner.web;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.SeleniumFactory;
import com.blueprintqa.runner.model.GraphNode;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ProbeController {
    private final SeleniumFactory selenium;
    private final ElementFinder finder;

    public ProbeController(SeleniumFactory selenium, ElementFinder finder) {
        this.selenium = selenium;
        this.finder = finder;
    }

    @PostMapping("/internal/probe")
    public Map<String, Object> probe(@RequestBody Map<String, Object> body) {
        GraphNode start = new GraphNode();
        start.data = new LinkedHashMap<>();
        start.data.put("browser", text(body.get("browser"), "chrome"));
        start.data.put("headless", Boolean.TRUE.equals(body.get("headless")) || "true".equalsIgnoreCase(String.valueOf(body.get("headless"))));
        start.data.put("windowWidth", 1280);
        start.data.put("windowHeight", 800);
        WebDriver driver = null;
        try {
            driver = selenium.open(start, "probe", 2);
            String url = text(body.get("url"), "");
            if (url.isBlank()) {
                throw new IllegalArgumentException("Probe needs a URL");
            }
            driver.navigate().to(url);
            String strategy = text(body.get("strategy"), "XPATH");
            String query = text(body.get("query"), "");
            int timeout = body.get("timeoutSeconds") instanceof Number number ? number.intValue() : 10;
            var element = finder.find(driver, strategy, query, timeout);
            List<String> samples = new ArrayList<>();
            String sample = element.getText();
            if (sample != null && !sample.isBlank()) {
                samples.add(sample.length() > 240 ? sample.substring(0, 240) : sample);
            }
            Map<String, Object> result = base(true, driver);
            result.put("count", 1);
            result.put("samples", samples);
            result.put("error", null);
            return result;
        } catch (Exception ex) {
            Map<String, Object> result = base(false, driver);
            result.put("count", 0);
            result.put("samples", List.of());
            result.put("error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
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

    private Map<String, Object> base(boolean ok, WebDriver driver) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", ok);
        result.put("currentUrl", safeUrl(driver));
        result.put("title", safeTitle(driver));
        result.put("screenshotBase64", shot(driver));
        return result;
    }

    private String shot(WebDriver driver) {
        if (!(driver instanceof TakesScreenshot screenshot)) {
            return "";
        }
        try {
            return screenshot.getScreenshotAs(OutputType.BASE64);
        } catch (Exception ex) {
            return "";
        }
    }

    private String safeUrl(WebDriver driver) {
        try {
            return driver == null ? "" : driver.getCurrentUrl();
        } catch (Exception ex) {
            return "";
        }
    }

    private String safeTitle(WebDriver driver) {
        try {
            return driver == null ? "" : driver.getTitle();
        } catch (Exception ex) {
            return "";
        }
    }

    private String text(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
