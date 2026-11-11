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
        if (context.elementInputCurrent() && usable(element)) {
            return element;
        }
        ElementRef ref = context.elementRef();
        if (ref != null && (!ref.query.isBlank() || !ref.selectors.isEmpty())) {
            return findRef(ref, context, finder);
        }
        return find(node, context, finder, "This step needs an element pin or a selector. Use Select on page.");
    }

    private static WebElement findRef(ElementRef ref, ExecutionContext context, ElementFinder finder) {
        GraphNode synthetic = new GraphNode();
        synthetic.data = new java.util.HashMap<>();
        synthetic.data.put("strategy", ref.strategy);
        synthetic.data.put("query", ref.query);
        synthetic.data.put("xpath", ref.query);
        synthetic.data.put("selectors", ref.selectors);
        synthetic.data.put("timeoutSeconds", 10);
        return find(synthetic, context, finder, "The element pin could not be found again in this browser.");
    }

    private static boolean usable(WebElement element) {
        if (element == null) {
            return false;
        }
        try {
            element.isEnabled();
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static WebElement find(GraphNode node, ExecutionContext context, ElementFinder finder, String emptyMessage) {
        List<String[]> candidates = candidates(node, context);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        int timeout = Math.max(2, node.integer("timeoutSeconds", 10));
        try {
            return pass(candidates, timeout, context, finder, emptyMessage);
        } catch (RuntimeException ex) {
            if (ex instanceof IllegalArgumentException || finder.documentComplete(context.driver())) {
                throw ex;
            }
            Pointer.settle(context.driver());
            return pass(candidates, timeout, context, finder, emptyMessage);
        }
    }

    private static WebElement pass(List<String[]> candidates, int timeout, ExecutionContext context, ElementFinder finder, String emptyMessage) {
        int[] budgets = SelectorBudget.allocate(candidates.size(), timeout);
        RuntimeException last = null;
        for (int index = 0; index < candidates.size(); index++) {
            String[] candidate = candidates.get(index);
            int slice = index < budgets.length ? budgets[index] : SelectorBudget.PROBE_SECONDS;
            try {
                return finder.findOnce(context.driver(), candidate[0], candidate[1], Math.max(1, slice));
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
