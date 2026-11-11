package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.interactions.Actions;
import org.springframework.stereotype.Component;

@Component
public class HoverNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public HoverNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "HOVER";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        new Actions(context.driver()).moveToElement(StepTargets.require(node, context, finder)).perform();
        return NodeOutput.pass("Hovered");
    }
}
