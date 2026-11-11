package com.blueprintqa.runner.engine;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

@Component
public class ElementFinder {
    private static final String TEXT_SCRIPT = """
            const needle = String(arguments[0] || '').toLowerCase();
            const all = document.body ? document.body.querySelectorAll('*') : [];
            let best = null;
            let bestLen = Infinity;
            for (const el of all) {
              const text = (el.innerText || '').trim().toLowerCase();
              if (!text || !text.includes(needle)) continue;
              if (text.length < bestLen) { best = el; bestLen = text.length; }
            }
            return best;
            """;

    private static final String REGEX_SCRIPT = """
            let re;
            try { re = new RegExp(String(arguments[0] || ''), 'i'); }
            catch (e) { return null; }
            const all = document.body ? document.body.querySelectorAll('*') : [];
            let best = null;
            let bestLen = Infinity;
            for (const el of all) {
              const text = (el.innerText || '').trim();
              if (!text || !re.test(text)) continue;
              if (text.length < bestLen) { best = el; bestLen = text.length; }
            }
            return best;
            """;

    public WebElement find(WebDriver driver, String strategy, String query, int timeoutSeconds) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("This step needs a selector or search text");
        }
        String kind = strategy == null ? "XPATH" : strategy.trim().toUpperCase(Locale.ROOT);
        int timeout = Math.max(1, timeoutSeconds);
        try {
            return switch (kind) {
                case "CSS" -> wait(driver, timeout).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(query)));
                case "TEXT_CONTAINS" -> waitJs(driver, timeout, TEXT_SCRIPT, query);
                case "REGEX_DOM" -> waitJs(driver, timeout, REGEX_SCRIPT, query);
                case "XPATH" -> wait(driver, timeout).until(ExpectedConditions.presenceOfElementLocated(By.xpath(query)));
                default -> throw new IllegalArgumentException("Unknown search strategy " + strategy);
            };
        } catch (TimeoutException ex) {
            throw new IllegalStateException("Could not find " + kind + " `" + query + "` within " + timeout + "s", ex);
        }
    }

    private WebDriverWait wait(WebDriver driver, int timeout) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

    private WebElement waitJs(WebDriver driver, int timeout, String script, String query) {
        return wait(driver, timeout).until(webDriver -> {
            Object found = ((JavascriptExecutor) webDriver).executeScript(script, query);
            return found instanceof WebElement element ? element : null;
        });
    }
}
