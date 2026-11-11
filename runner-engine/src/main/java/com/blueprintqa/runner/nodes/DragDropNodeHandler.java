package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.springframework.stereotype.Component;

@Component
public class DragDropNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public DragDropNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "DRAG_DROP";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        WebElement source = StepTargets.require(node, context, finder);
        String targetQuery = context.interpolate(node.str("targetXpath", node.str("targetQuery", "")));
        if (targetQuery == null || targetQuery.isBlank()) {
            throw new IllegalArgumentException("Drag and drop needs a target selector");
        }
        WebElement target = finder.find(context.driver(), node.str("targetStrategy", "XPATH"), targetQuery, node.integer("timeoutSeconds", 10));
        new Actions(context.driver()).dragAndDrop(source, target).perform();
        return NodeOutput.pass("Dropped");
    }
}
