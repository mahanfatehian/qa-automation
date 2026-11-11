package com.blueprintqa.runner.engine;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BrowserCheckpoint {
    private static final String CAPTURE = """
            const dump = (store) => {
              const out = {};
              try {
                for (let i = 0; i < store.length; i++) {
                  const key = store.key(i);
                  out[key] = store.getItem(key);
                }
              } catch (e) {}
              return out;
            };
            const fields = [];
            try {
              document.querySelectorAll('input, textarea, select').forEach((el, index) => {
                fields.push({
                  index,
                  id: el.id || '',
                  name: el.getAttribute('name') || '',
                  type: (el.getAttribute('type') || el.tagName || '').toLowerCase(),
                  value: el.value || '',
                  checked: !!el.checked
                });
              });
            } catch (e) {}
            return JSON.stringify({
              local: dump(window.localStorage),
              session: dump(window.sessionStorage),
              fields,
              x: window.scrollX || 0,
              y: window.scrollY || 0
            });
            """;

    private static final String RESTORE_STORAGE = """
            const state = JSON.parse(arguments[0] || '{}');
            const load = (store, data) => {
              if (!data) return;
              store.clear();
              Object.entries(data).forEach(([key, value]) => store.setItem(key, value == null ? '' : String(value)));
            };
            load(window.localStorage, state.local);
            load(window.sessionStorage, state.session);
            """;

    private static final String RESTORE_FIELDS = """
            const state = JSON.parse(arguments[0] || '{}');
            const nodes = document.querySelectorAll('input, textarea, select');
            for (const field of state.fields || []) {
              let el = null;
              if (field.id) el = document.getElementById(field.id);
              if (!el && field.name) {
                try { el = document.querySelector(`[name="${CSS.escape(field.name)}"]`); } catch (e) { el = null; }
              }
              if (!el && Number.isInteger(field.index)) el = nodes[field.index] || null;
              if (!el) continue;
              if (field.type === 'checkbox' || field.type === 'radio') el.checked = !!field.checked;
              else if ('value' in el) el.value = field.value || '';
              el.dispatchEvent(new Event('input', { bubbles: true }));
              el.dispatchEvent(new Event('change', { bubbles: true }));
            }
            window.scrollTo(state.x || 0, state.y || 0);
            """;

    public final String url;
    public final int width;
    public final int height;
    public final List<Cookie> cookies;
    public final String stateJson;
    public final List<String> tabUrls;
    public final int currentTab;

    private BrowserCheckpoint(String url, int width, int height, List<Cookie> cookies, String stateJson, List<String> tabUrls, int currentTab) {
        this.url = url == null ? "" : url;
        this.width = width;
        this.height = height;
        this.cookies = List.copyOf(cookies == null ? List.of() : cookies);
        this.stateJson = stateJson == null || stateJson.isBlank() ? "{}" : stateJson;
        this.tabUrls = List.copyOf(tabUrls == null || tabUrls.isEmpty() ? List.of(this.url) : tabUrls);
        this.currentTab = Math.max(0, Math.min(currentTab, Math.max(0, this.tabUrls.size() - 1)));
    }

    public static BrowserCheckpoint capture(WebDriver driver) {
        String origin;
        try {
            origin = driver.getWindowHandle();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Cannot capture a browser checkpoint", ex);
        }
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        List<String> tabUrls = new ArrayList<>();
        int current = 0;
        Map<String, Cookie> cookies = new LinkedHashMap<>();
        try {
            for (int index = 0; index < handles.size(); index++) {
                driver.switchTo().window(handles.get(index));
                tabUrls.add(readUrl(driver));
                if (handles.get(index).equals(origin)) {
                    current = index;
                }
                try {
                    for (Cookie cookie : driver.manage().getCookies()) {
                        cookies.put(cookie.getName() + "|" + cookie.getDomain() + "|" + cookie.getPath(), cookie);
                    }
                } catch (RuntimeException ignored) {
                    // this tab had no readable cookie jar; other tabs still count
                }
            }
        } finally {
            try {
                driver.switchTo().window(origin);
            } catch (RuntimeException ignored) {
                // the caller still has the original handle if the session survived
            }
        }
        String url = current < tabUrls.size() ? tabUrls.get(current) : readUrl(driver);
        int width = 1280;
        int height = 800;
        try {
            Dimension size = driver.manage().window().getSize();
            width = Math.max(320, size.width);
            height = Math.max(240, size.height);
        } catch (RuntimeException ignored) {
            width = 1280;
            height = 800;
        }
        String state = "{}";
        try {
            Object raw = ((JavascriptExecutor) driver).executeScript(CAPTURE);
            if (raw != null) {
                state = String.valueOf(raw);
            }
        } catch (RuntimeException ignored) {
            state = "{}";
        }
        return new BrowserCheckpoint(url, width, height, new ArrayList<>(cookies.values()), state, tabUrls, current);
    }

    public void restore(WebDriver driver) {
        if (blank(url)) {
            throw new IllegalStateException("Clone did not land. The checkpoint page is blank, so this branch will not continue from about:blank.");
        }
        String origin = driver.getWindowHandle();
        openPage(driver, url);
        applyCookies(driver, url);
        driver.navigate().to(url);
        waitReady(driver);
        try {
            ((JavascriptExecutor) driver).executeScript(RESTORE_STORAGE, stateJson);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Could not restore page storage for " + url, ex);
        }
        driver.navigate().refresh();
        waitReady(driver);
        try {
            ((JavascriptExecutor) driver).executeScript(RESTORE_FIELDS, stateJson);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Could not restore form fields for " + url, ex);
        }
        try {
            driver.manage().window().setSize(new Dimension(width, height));
        } catch (RuntimeException ignored) {
            // grid may ignore window size
        }
        for (int index = 0; index < tabUrls.size(); index++) {
            if (index == currentTab) {
                continue;
            }
            String tabUrl = tabUrls.get(index);
            driver.switchTo().newWindow(WindowType.TAB);
            if (blank(tabUrl)) {
                continue;
            }
            openPage(driver, tabUrl);
            applyCookies(driver, tabUrl);
            driver.navigate().to(tabUrl);
            waitReady(driver);
        }
        driver.switchTo().window(origin);
        waitReady(driver);
        String landed = readUrl(driver);
        if (!pageLanded(url, landed)) {
            String where = blank(landed) ? "a blank page" : landed;
            throw new IllegalStateException("Clone did not land on " + url + ". The browser is on " + where + ".");
        }
    }

    public static boolean pageLanded(String expected, String actual) {
        if (blank(expected) || blank(actual)) {
            return false;
        }
        return canonical(expected).equals(canonical(actual));
    }

    private void openPage(WebDriver driver, String pageUrl) {
        driver.navigate().to(pageUrl);
        waitReady(driver);
        String landed = readUrl(driver);
        if (blank(landed)) {
            throw new IllegalStateException("Clone did not land. The browser is still on a blank page.");
        }
    }

    private void applyCookies(WebDriver driver, String pageUrl) {
        for (Cookie cookie : cookies) {
            if (cookie == null || expired(cookie) || !applies(cookie, pageUrl)) {
                continue;
            }
            try {
                driver.manage().addCookie(cookie);
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Could not restore cookie " + cookie.getName() + " for " + pageUrl, ex);
            }
        }
    }

    private void waitReady(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(webDriver ->
                    "complete".equals(((JavascriptExecutor) webDriver).executeScript("return document.readyState"))
            );
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Clone page did not become ready", ex);
        }
    }

    private static boolean applies(Cookie cookie, String pageUrl) {
        String domain = cookie.getDomain();
        if (domain == null || domain.isBlank()) {
            return true;
        }
        String host = host(pageUrl);
        if (host.isBlank()) {
            return false;
        }
        String normalized = domain.startsWith(".") ? domain.substring(1) : domain;
        return host.equalsIgnoreCase(normalized) || host.toLowerCase().endsWith("." + normalized.toLowerCase());
    }

    private static boolean expired(Cookie cookie) {
        Date expiry = cookie.getExpiry();
        return expiry != null && expiry.before(new Date());
    }

    private static String host(String pageUrl) {
        try {
            String host = URI.create(pageUrl).getHost();
            return host == null ? "" : host;
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private static String readUrl(WebDriver driver) {
        try {
            String current = driver.getCurrentUrl();
            return current == null ? "" : current;
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank() || value.startsWith("about:") || value.startsWith("data:");
    }

    private static String canonical(String value) {
        String text = value.trim();
        if (text.length() > "http://x/".length() && text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
