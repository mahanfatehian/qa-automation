package com.blueprintqa.runner.engine;

import com.blueprintqa.runner.model.GraphNode;
import org.openqa.selenium.WebElement;

public final class StepTargets {
    private StepTargets() {
    }

    public static WebElement require(GraphNode node, ExecutionContext context, ElementFinder finder) {
        WebElement element = context.elementInput();
        if (element != null) {
            return element;
        }
        String query = context.interpolate(node.str("xpath", node.str("query", "")));
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("This step needs an element pin or a selector");
        }
        return finder.find(context.driver(), node.str("strategy", "XPATH"), query, node.integer("timeoutSeconds", 10));
    }
}
