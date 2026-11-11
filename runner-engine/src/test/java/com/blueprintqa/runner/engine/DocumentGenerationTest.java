package com.blueprintqa.runner.engine;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentGenerationTest {
    @Test
    void invalidateDropsLiveElementAndKeepsRef() {
        ExecutionContext context = new ExecutionContext(null, Map.of());
        WebElement element = element();
        ElementRef ref = new ElementRef("CSS", "#go", List.of(Map.of("kind", "CSS", "value", "#go")));
        context.putInput("element_in", element);
        context.putInput("element_in_ref", ref);
        context.setPin("search", "element_out", element);
        context.setPin("search", "element_out_ref", ref);
        assertSame(element, context.elementInput());
        assertTrue(context.elementInputCurrent());
        context.invalidateElements();
        assertNull(context.elementInput());
        assertFalse(context.elementInputCurrent());
        assertSame(ref, context.elementRef());
        assertNull(context.getPin("search", "element_out"));
        assertSame(ref, context.getPin("search", "element_out_ref"));
    }

    @Test
    void forkCopiesElementRefNotTheLiveElement() {
        ExecutionContext context = new ExecutionContext(null, Map.of());
        WebElement element = element();
        ElementRef ref = new ElementRef("XPATH", "//button");
        context.setPin("search", "element_out", element);
        context.setPin("search", "element_out_ref", ref);
        ExecutionContext copy = context.fork(null, "branch 2");
        assertNull(copy.getPin("search", "element_out"));
        assertSame(ref, copy.getPin("search", "element_out_ref"));
    }

    private static WebElement element() {
        return (WebElement) Proxy.newProxyInstance(
                WebElement.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    if ("isEnabled".equals(method.getName()) || "isDisplayed".equals(method.getName())) {
                        return true;
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (method.getReturnType().equals(int.class)) {
                        return 0;
                    }
                    if (method.getReturnType().equals(long.class)) {
                        return 0L;
                    }
                    return null;
                });
    }
}
