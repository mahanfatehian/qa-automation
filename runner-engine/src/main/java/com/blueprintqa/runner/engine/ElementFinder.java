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
    public static final ThreadLocal<Integer> INSPECT_CAP = new ThreadLocal<>();

    private static final String CSS_SCRIPT = """
            const selector = String(arguments[0] || '');
            const walk = (root) => {
              if (!root || !root.querySelector) return null;
              let found = null;
              try { found = root.querySelector(selector); } catch (e) { return null; }
              if (found) return found;
              const nodes = root.querySelectorAll('*');
              for (const node of nodes) {
                if (!node.shadowRoot) continue;
                const inner = walk(node.shadowRoot);
                if (inner) return inner;
              }
              return null;
            };
            return walk(document);
            """;

    private static final String TEXT_SCRIPT = """
            const needle = String(arguments[0] || '').toLowerCase();
            const scan = (root) => {
              if (!root || !root.querySelectorAll) return null;
              let best = null;
              let bestLen = Infinity;
              const nodes = root.querySelectorAll('a,button,label,h1,h2,h3,h4,p,li,td,th,span,option');
              for (const el of nodes) {
                const text = (el.textContent || '').trim().toLowerCase();
                if (!text || text.length > 180 || !text.includes(needle) || text.length >= bestLen) continue;
                best = el;
                bestLen = text.length;
              }
              return best;
            };
            return scan(document) || scan(document.body) || (() => {
              for (const host of document.querySelectorAll('*')) {
                if (!host.shadowRoot) continue;
                const found = scan(host.shadowRoot);
                if (found) return found;
              }
              return null;
            })();
            """;

    private static final String REGEX_SCRIPT = """
            let re;
            try { re = new RegExp(String(arguments[0] || ''), 'i'); }
            catch (e) { return null; }
            const state = { el: null, len: Infinity, seen: 0 };
            const visit = (root) => {
              if (!root || !root.querySelectorAll || state.seen > 4000) return;
              for (const el of root.querySelectorAll('*')) {
                if (state.seen++ > 4000) return;
                const text = (el.innerText || el.textContent || '').trim();
                if (text && re.test(text) && text.length < state.len) {
                  state.el = el;
                  state.len = text.length;
                }
                if (el.shadowRoot) visit(el.shadowRoot);
              }
            };
            visit(document);
            return state.el;
            """;

    private static final String ROLE_SCRIPT = """
            const raw = String(arguments[0] || '');
            const cut = raw.indexOf('|');
            const wantRole = (cut < 0 ? raw : raw.slice(0, cut)).trim().toLowerCase();
            const wantName = (cut < 0 ? '' : raw.slice(cut + 1)).trim().toLowerCase();
            const roleOf = (el) => {
              const explicit = (el.getAttribute('role') || '').toLowerCase();
              if (explicit) return explicit;
              const tag = el.tagName.toLowerCase();
              if (tag === 'a' && el.hasAttribute('href')) return 'link';
              if (tag === 'button') return 'button';
              if (tag === 'textarea') return 'textbox';
              if (tag === 'select') return 'combobox';
              if (tag === 'img') return 'img';
              if (tag === 'input') {
                const type = (el.getAttribute('type') || 'text').toLowerCase();
                if (type === 'checkbox') return 'checkbox';
                if (type === 'radio') return 'radio';
                if (type === 'submit' || type === 'button') return 'button';
                return 'textbox';
              }
              return '';
            };
            const accName = (el) => {
              const aria = el.getAttribute('aria-label');
              if (aria) return aria.trim();
              const title = el.getAttribute('title') || el.getAttribute('alt') || el.getAttribute('name') || '';
              if (title.trim()) return title.trim();
              return (el.innerText || '').replace(/\\s+/g, ' ').trim().slice(0, 80);
            };
            const matches = (el) => {
              const role = roleOf(el);
              const name = accName(el).toLowerCase();
              if (wantRole && wantRole !== '*' && role !== wantRole) return false;
              if (wantName && !name.includes(wantName)) return false;
              return !!(wantRole || wantName);
            };
            let best = null;
            let bestArea = Infinity;
            let seen = 0;
            const visit = (root) => {
              if (!root || !root.querySelectorAll || seen > 4000) return;
              for (const el of root.querySelectorAll('*')) {
                if (seen++ > 4000) return;
                if (!matches(el)) {
                  if (el.shadowRoot) visit(el.shadowRoot);
                  continue;
                }
                const box = el.getBoundingClientRect();
                const area = Math.max(1, box.width * box.height);
                if (area < bestArea) { best = el; bestArea = area; }
                if (el.shadowRoot) visit(el.shadowRoot);
              }
            };
            visit(document);
            return best;
            """;

    public WebElement hit(WebDriver driver, String strategy, String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String kind = strategy == null ? "XPATH" : strategy.trim().toUpperCase(Locale.ROOT);
        try {
            if ("CSS".equals(kind) || "TEXT_CONTAINS".equals(kind) || "REGEX_DOM".equals(kind) || "ROLE".equals(kind)) {
                String script = switch (kind) {
                    case "TEXT_CONTAINS" -> TEXT_SCRIPT;
                    case "REGEX_DOM" -> REGEX_SCRIPT;
                    case "ROLE" -> ROLE_SCRIPT;
                    default -> CSS_SCRIPT;
                };
                Object found = ((JavascriptExecutor) driver).executeScript(script, query);
                return found instanceof WebElement element && interactable(element) ? element : null;
            }
            By by = by(kind, query);
            if (by == null) {
                return null;
            }
            for (WebElement element : driver.findElements(by)) {
                if (interactable(element)) {
                    return element;
                }
            }
            return null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public WebElement find(WebDriver driver, String strategy, String query, int timeoutSeconds) {
        return findOnce(driver, strategy, query, timeoutSeconds);
    }

    public WebElement findOnce(WebDriver driver, String strategy, String query, int timeoutSeconds) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("This step needs a selector or search text");
        }
        String kind = strategy == null ? "XPATH" : strategy.trim().toUpperCase(Locale.ROOT);
        int timeout = Math.max(1, timeoutSeconds);
        Integer cap = INSPECT_CAP.get();
        if (cap != null) {
            timeout = Math.min(timeout, cap);
        }
        try {
            if ("CSS".equals(kind)) {
                return waitJs(driver, timeout, CSS_SCRIPT, query);
            }
            if ("ROLE".equals(kind)) {
                return waitJs(driver, timeout, ROLE_SCRIPT, query);
            }
            By by = by(kind, query);
            if (by == null) {
                return switch (kind) {
                    case "TEXT_CONTAINS" -> waitJs(driver, timeout, TEXT_SCRIPT, query);
                    case "REGEX_DOM" -> waitJs(driver, timeout, REGEX_SCRIPT, query);
                    default -> throw new IllegalArgumentException("Unknown search strategy " + strategy);
                };
            }
            return wait(driver, timeout).until(ExpectedConditions.elementToBeClickable(by));
        } catch (TimeoutException ex) {
            throw new IllegalStateException("Could not find " + kind + " `" + query + "` within " + timeout + "s", ex);
        }
    }

    public boolean documentComplete(WebDriver driver) {
        try {
            Object state = ((JavascriptExecutor) driver).executeScript("return document.readyState");
            return "complete".equals(state);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public By by(String strategy, String query) {
        String kind = strategy == null ? "XPATH" : strategy.trim().toUpperCase(Locale.ROOT);
        return switch (kind) {
            case "CSS" -> By.cssSelector(query);
            case "CLASS" -> By.className(query);
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

    private boolean interactable(WebElement element) {
        try {
            return element.isDisplayed() && element.isEnabled();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private WebDriverWait wait(WebDriver driver, int timeout) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

    private WebElement waitJs(WebDriver driver, int timeout, String script, String query) {
        return wait(driver, timeout).until(webDriver -> {
            Object found = ((JavascriptExecutor) webDriver).executeScript(script, query);
            if (!(found instanceof WebElement element) || !interactable(element)) {
                return null;
            }
            return element;
        });
    }
}
