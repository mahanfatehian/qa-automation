package com.blueprintqa.runner.engine;

import com.blueprintqa.runner.model.GraphNode;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StepTargets {
    private StepTargets() {
    }

    public static WebElement require(GraphNode node, ExecutionContext context, ElementFinder finder) {
        WebElement element = context.elementInput();
        if (element != null) {
            return element;
        }
        return find(node, context, finder, "This step needs an element pin or a selector. Use Select on page.");
    }

    public static WebElement find(GraphNode node, ExecutionContext context, ElementFinder finder, String emptyMessage) {
        List<String[]> candidates = candidates(node, context);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        int timeout = Math.max(2, node.integer("timeoutSeconds", 10));
        int each = Math.max(2, timeout / candidates.size());
        RuntimeException last = null;
        for (String[] candidate : candidates) {
            try {
                return finder.find(context.driver(), candidate[0], candidate[1], each);
            } catch (RuntimeException ex) {
                last = ex;
            }
        }
        throw last == null ? new IllegalStateException(emptyMessage) : last;
    }

    private static List<String[]> candidates(GraphNode node, ExecutionContext context) {
        List<String[]> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Object raw = node.data == null ? null : node.data.get("selectors");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                add(out, seen, text(map.get("kind"), "CSS"), context.interpolate(text(map.get("value"), "")));
            }
        }
        String query = context.interpolate(node.str("query", ""));
        String xpath = context.interpolate(node.str("xpath", ""));
        String strategy = node.str("strategy", query.isBlank() ? "XPATH" : "CSS");
        add(out, seen, strategy, query.isBlank() ? xpath : query);
        if (!xpath.isBlank() && !xpath.equals(query)) {
            add(out, seen, "XPATH", xpath);
        }
        return out;
    }

    private static void add(List<String[]> out, Set<String> seen, String kind, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String key = kind.toUpperCase() + "\0" + value;
        if (seen.add(key)) {
            out.add(new String[]{kind, value});
        }
    }

    private static String text(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
