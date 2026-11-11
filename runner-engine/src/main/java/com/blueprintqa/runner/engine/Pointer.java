package com.blueprintqa.runner.engine;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class Pointer {
    private Pointer() {
    }

    public static String click(WebDriver driver, WebElement element) {
        return click(driver, element, null);
    }

    public static String click(WebDriver driver, WebElement element, ExecutionContext context) {
        String before = url(driver);
        long beforeMut = mark(driver);
        boolean beforeChecked = checked(driver, element);
        scroll(driver, element);
        aim(driver, element);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(4)).until(ExpectedConditions.elementToBeClickable(element));
        } catch (Exception ignored) {
            // still try the click; a covered control is handled below
        }
        try {
            if (covered(driver, element)) {
                jsClick(driver, element);
            } else {
                new Actions(driver).moveToElement(element).click().perform();
            }
        } catch (Exception ex) {
            jsClick(driver, element);
        }
        if (changed(driver, element, before, beforeMut, beforeChecked)) {
            return accepted(driver, before, context);
        }
        jsClick(driver, element);
        if (changed(driver, element, before, beforeMut, beforeChecked)) {
            return accepted(driver, before, context);
        }
        throw new IllegalStateException("Click did not land. The control was found, but the page did not react.");
    }

    public static void type(WebDriver driver, WebElement element, String text, boolean clearFirst) {
        ready(driver);
        scroll(driver, element);
        ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", element);
        if (clearFirst) {
            try {
                element.clear();
            } catch (Exception ex) {
                element.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"), org.openqa.selenium.Keys.DELETE);
            }
        }
        if (!text.isEmpty()) {
            element.sendKeys(text);
        }
        String value = readValue(driver, element);
        if (!text.isEmpty() && !value.contains(text)) {
            ((JavascriptExecutor) driver).executeScript("""
                    const el = arguments[0];
                    const value = arguments[1];
                    const proto = Object.getPrototypeOf(el);
                    const setter = Object.getOwnPropertyDescriptor(proto, 'value')?.set;
                    if (setter) setter.call(el, value); else el.value = value;
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    """, element, text);
            value = readValue(driver, element);
        }
        if (!text.isEmpty() && !value.contains(text)) {
            throw new IllegalStateException("Type did not land. The field still reads \"" + value + "\".");
        }
    }

    public static void choose(WebElement element, String choice, boolean byValue) {
        org.openqa.selenium.support.ui.Select select = new org.openqa.selenium.support.ui.Select(element);
        if (byValue) {
            select.selectByValue(choice);
        } else {
            select.selectByVisibleText(choice);
        }
        WebElement picked = select.getFirstSelectedOption();
        String shown = picked.getText() == null ? "" : picked.getText().trim();
        String value = picked.getAttribute("value") == null ? "" : picked.getAttribute("value");
        if (!choice.equals(shown) && !choice.equals(value)) {
            throw new IllegalStateException("Select did not land. The control still shows \"" + shown + "\".");
        }
    }

    public static boolean check(WebDriver driver, WebElement element, boolean want) {
        return check(driver, element, want, null);
    }

    public static boolean check(WebDriver driver, WebElement element, boolean want, ExecutionContext context) {
        ready(driver);
        if (element.isSelected() != want) {
            click(driver, element, context);
        }
        if (element.isSelected() != want) {
            throw new IllegalStateException(want ? "Check did not land." : "Uncheck did not land.");
        }
        return element.isSelected();
    }

    public static void hover(WebDriver driver, WebElement element) {
        scroll(driver, element);
        aim(driver, element);
        new Actions(driver).moveToElement(element).perform();
    }

    public static void aim(WebDriver driver, WebElement element) {
        try {
            Object raw = ((JavascriptExecutor) driver).executeScript("""
                    const el = arguments[0];
                    const box = el.getBoundingClientRect();
                    return [Math.round(box.left + box.width / 2), Math.round(box.top + box.height / 2)];
                    """, element);
            if (raw instanceof java.util.List<?> list && list.size() >= 2) {
                POINT.set(new int[]{number(list.get(0)), number(list.get(1))});
            }
        } catch (Exception ignored) {
            POINT.remove();
        }
    }

    public static void paint(WebDriver driver) {
        int[] point = POINT.get();
        if (point == null) {
            return;
        }
        try {
            ((JavascriptExecutor) driver).executeScript(CURSOR, point[0], point[1]);
        } catch (Exception ignored) {
            // a closed page still gets a plain screenshot
        }
    }

    public static void wipe(WebDriver driver) {
        try {
            ((JavascriptExecutor) driver).executeScript("document.getElementById('__bp_cursor')?.remove()");
        } catch (Exception ignored) {
            // page may already be gone
        }
    }

    private static final ThreadLocal<int[]> POINT = new ThreadLocal<>();
    private static final String CURSOR = """
            const x = arguments[0], y = arguments[1];
            let el = document.getElementById('__bp_cursor');
            if (!el) {
              el = document.createElement('div');
              el.id = '__bp_cursor';
              el.setAttribute('style', 'position:fixed;z-index:2147483647;pointer-events:none;width:28px;height:28px;');
              el.innerHTML = '<svg width="28" height="28" viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg"><circle cx="4" cy="3" r="8" fill="none" stroke="#7f6df2" stroke-width="2"/><path d="M3 1 L3 17 L8 12.5 L11.5 20 L14.5 18.5 L10.5 11.5 L17 11 Z" fill="#111" stroke="#fff" stroke-width="1.2"/></svg>';
              (document.documentElement || document.body).appendChild(el);
            }
            el.style.left = (x - 3) + 'px';
            el.style.top = (y - 1) + 'px';
            """;

    public static void ready(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(4)).until(d ->
                    "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState"))
            );
        } catch (Exception ignored) {
            // a site that never finishes loading can still be acted on; the action itself must land
        }
    }

    private static String readValue(WebDriver driver, WebElement element) {
        try {
            Object value = ((JavascriptExecutor) driver).executeScript("return arguments[0].value ?? ''", element);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception ex) {
            return "";
        }
    }

    public static void settle(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(4)).until(d ->
                    "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState"))
            );
        } catch (Exception ignored) {
            // a site that never finishes loading can still be acted on; the shot must not be the spinner
        }
        int cap = Math.max(6000, Math.min(12000, Math.max(GridBudget.readyMs, 6000)));
        long start = System.currentTimeMillis();
        int steady = 0;
        while (System.currentTimeMillis() - start < cap) {
            if (painted(driver)) {
                steady++;
                if (steady >= 2) {
                    return;
                }
            } else {
                steady = 0;
            }
            try {
                Thread.sleep(180L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public static boolean painted(WebDriver driver) {
        try {
            Object value = ((JavascriptExecutor) driver).executeScript("""
                    const text = (document.body && document.body.innerText || '').replace(/\\s+/g, ' ').trim();
                    const main = document.querySelector('main, [role="main"], #__next, #app');
                    const mainText = (main && main.innerText || '').replace(/\\s+/g, ' ').trim();
                    const center = document.elementFromPoint(Math.floor(window.innerWidth / 2), Math.floor(window.innerHeight / 2));
                    const box = center ? center.getBoundingClientRect() : null;
                    const centerText = (center && (center.innerText || center.textContent) || '').trim();
                    const tinyCenter = !!(box && box.width < 120 && box.height < 80 && centerText.length < 4);
                    const busy = !!document.querySelector('[aria-busy="true"], [class*="skeleton" i], [class*="Spinner" i], [class*="loading" i]');
                    if (text.length < 40) return false;
                    if (tinyCenter && mainText.length < 40) return false;
                    if (busy && mainText.length < 80) return false;
                    return mainText.length > 40 || text.length > 180;
                    """);
            return Boolean.TRUE.equals(value);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static void scroll(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", element);
    }

    private static void jsClick(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("""
                const el = arguments[0];
                el.focus && el.focus();
                el.click();
                """, element);
    }

    private static boolean covered(WebDriver driver, WebElement element) {
        Object hit = ((JavascriptExecutor) driver).executeScript("""
                const el = arguments[0];
                const box = el.getBoundingClientRect();
                const x = box.left + box.width / 2;
                const y = box.top + box.height / 2;
                const top = document.elementFromPoint(x, y);
                if (!top) return true;
                return top !== el && !el.contains(top) && !top.contains(el);
                """, element);
        return Boolean.TRUE.equals(hit);
    }

    private static long mark(WebDriver driver) {
        Object value = ((JavascriptExecutor) driver).executeScript("""
                window.__bpMut = 0;
                if (!window.__bpObs) {
                  window.__bpObs = new MutationObserver(() => { window.__bpMut++; });
                  window.__bpObs.observe(document.documentElement, { subtree: true, childList: true, attributes: true });
                }
                return window.__bpMut;
                """);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static String accepted(WebDriver driver, String before, ExecutionContext context) {
        if (ClickLanding.urlChanged(before, url(driver))) {
            settle(driver);
        }
        if (context != null) {
            context.invalidateElements();
        }
        return "Clicked";
    }

    private static boolean changed(WebDriver driver, WebElement element, String before, long beforeMut, boolean beforeChecked) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 1200L) {
            if (reacted(driver, element, before, beforeMut, beforeChecked)) {
                return true;
            }
            try {
                Thread.sleep(80L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return reacted(driver, element, before, beforeMut, beforeChecked);
    }

    private static boolean reacted(WebDriver driver, WebElement element, String before, long beforeMut, boolean beforeChecked) {
        return ClickLanding.landed(before, url(driver), beforeMut, mutation(driver), beforeChecked, checked(driver, element));
    }

    private static long mutation(WebDriver driver) {
        try {
            Object value = ((JavascriptExecutor) driver).executeScript("return window.__bpMut || 0");
            return value instanceof Number number ? number.longValue() : 0L;
        } catch (RuntimeException ex) {
            return 0L;
        }
    }

    private static boolean checked(WebDriver driver, WebElement element) {
        try {
            if (element.isSelected()) {
                return true;
            }
        } catch (RuntimeException ex) {
            return false;
        }
        try {
            Object value = ((JavascriptExecutor) driver).executeScript(
                    "const el = arguments[0]; if (!el) return false; return !!(el.checked || el.getAttribute('aria-checked') === 'true');",
                    element);
            return Boolean.TRUE.equals(value);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String url(WebDriver driver) {
        try {
            String current = driver.getCurrentUrl();
            return current == null ? "" : current;
        } catch (Exception ex) {
            return "";
        }
    }
}
