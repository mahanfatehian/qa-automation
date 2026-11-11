package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.Pointer;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.interactions.Actions;
import org.springframework.stereotype.Component;

@Component
public class DoubleClickNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public DoubleClickNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "DOUBLE_CLICK";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        var element = StepTargets.require(node, context, finder);
        Pointer.aim(context.driver(), element);
        new Actions(context.driver()).doubleClick(element).perform();
        return NodeOutput.pass("Double clicked");
    }
}
