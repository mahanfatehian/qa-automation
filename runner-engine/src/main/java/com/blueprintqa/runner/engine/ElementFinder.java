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
            By by = by(kind, query);
            if (by == null) {
                return switch (kind) {
                    case "TEXT_CONTAINS" -> waitJs(driver, timeout, TEXT_SCRIPT, query);
                    case "REGEX_DOM" -> waitJs(driver, timeout, REGEX_SCRIPT, query);
                    default -> throw new IllegalArgumentException("Unknown search strategy " + strategy);
                };
            }
            return wait(driver, timeout).until(ExpectedConditions.presenceOfElementLocated(by));
        } catch (TimeoutException ex) {
            throw new IllegalStateException("Could not find " + kind + " `" + query + "` within " + timeout + "s", ex);
        }
    }

    public By by(String strategy, String query) {
        String kind = strategy == null ? "XPATH" : strategy.trim().toUpperCase(Locale.ROOT);
        return switch (kind) {
            case "CSS" -> By.cssSelector(query);
            case "ID" -> By.id(query);
            case "NAME" -> By.name(query);
            case "LINK_TEXT" -> By.linkText(query);
            case "PARTIAL_LINK_TEXT" -> By.partialLinkText(query);
            case "TAG" -> By.tagName(query);
            case "XPATH" -> By.xpath(query);
            default -> null;
        };
    }

    public int count(WebDriver driver, String strategy, String query) {
        By by = by(strategy, query);
        if (by == null) {
            throw new IllegalArgumentException("Count needs a locator strategy, not " + strategy);
        }
        return driver.findElements(by).size();
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
