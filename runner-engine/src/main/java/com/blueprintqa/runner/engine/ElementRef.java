package com.blueprintqa.runner.engine;

import com.blueprintqa.runner.model.GraphNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ElementRef {
    public final String strategy;
    public final String query;
    public final List<Map<String, String>> selectors;

    public ElementRef(String strategy, String query) {
        this(strategy, query, List.of());
    }

    public ElementRef(String strategy, String query, List<Map<String, String>> selectors) {
        this.strategy = strategy == null || strategy.isBlank() ? "CSS" : strategy;
        this.query = query == null ? "" : query;
        this.selectors = selectors == null ? List.of() : selectors;
    }

    public static ElementRef from(GraphNode node, ExecutionContext context) {
        List<Map<String, String>> selectors = new ArrayList<>();
        Object raw = node.data == null ? null : node.data.get("selectors");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                Map<String, String> selector = new LinkedHashMap<>();
                selector.put("kind", text(map.get("kind")));
                selector.put("value", context.interpolate(text(map.get("value"))));
                selectors.add(selector);
            }
        }
        String query = context.interpolate(node.str("query", node.str("xpath", "")));
        return new ElementRef(node.str("strategy", query.isBlank() ? "XPATH" : "CSS"), query, selectors);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
