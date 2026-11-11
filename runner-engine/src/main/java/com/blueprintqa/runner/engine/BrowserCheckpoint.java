package com.blueprintqa.runner.engine;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;

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
              try {
                store.clear();
                Object.entries(data).forEach(([key, value]) => store.setItem(key, value == null ? '' : String(value)));
              } catch (e) {}
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

    private BrowserCheckpoint(String url, int width, int height, List<Cookie> cookies, String stateJson) {
        this.url = url == null ? "" : url;
        this.width = width;
        this.height = height;
        this.cookies = cookies;
        this.stateJson = stateJson == null || stateJson.isBlank() ? "{}" : stateJson;
    }

    public static BrowserCheckpoint capture(WebDriver driver) {
        String url = "";
        try {
            url = driver.getCurrentUrl();
        } catch (Exception ignored) {
            url = "";
        }
        int width = 1280;
        int height = 800;
        try {
            Dimension size = driver.manage().window().getSize();
            width = Math.max(320, size.width);
            height = Math.max(240, size.height);
        } catch (Exception ignored) {
            width = 1280;
            height = 800;
        }
        List<Cookie> cookies = new ArrayList<>();
        try {
            cookies.addAll(driver.manage().getCookies());
        } catch (Exception ignored) {
            cookies.clear();
        }
        String state = "{}";
        try {
            Object raw = ((JavascriptExecutor) driver).executeScript(CAPTURE);
            if (raw != null) {
                state = String.valueOf(raw);
            }
        } catch (Exception ignored) {
            state = "{}";
        }
        return new BrowserCheckpoint(url, width, height, cookies, state);
    }

    public void restore(WebDriver driver) {
        if (url.isBlank() || url.startsWith("data:") || url.startsWith("about:")) {
            return;
        }
        driver.navigate().to(url);
        for (Cookie cookie : cookies) {
            try {
                driver.manage().addCookie(cookie);
            } catch (Exception ignored) {
                // domain or expiry rejected this cookie; the page still reloads from the URL
            }
        }
        driver.navigate().to(url);
        try {
            ((JavascriptExecutor) driver).executeScript(RESTORE_STORAGE, stateJson);
            driver.navigate().refresh();
            ((JavascriptExecutor) driver).executeScript(RESTORE_FIELDS, stateJson);
        } catch (Exception ignored) {
            // storage can be blocked; the URL and cookies are the clone
        }
        try {
            driver.manage().window().setSize(new Dimension(width, height));
        } catch (Exception ignored) {
            // grid may ignore window size
        }
    }
}
