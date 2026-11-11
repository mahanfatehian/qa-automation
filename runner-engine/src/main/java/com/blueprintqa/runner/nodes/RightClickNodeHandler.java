package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.interactions.Actions;
import org.springframework.stereotype.Component;

@Component
public class RightClickNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public RightClickNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "RIGHT_CLICK";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        new Actions(context.driver()).contextClick(StepTargets.require(node, context, finder)).perform();
        return NodeOutput.pass("Right clicked");
    }
}
