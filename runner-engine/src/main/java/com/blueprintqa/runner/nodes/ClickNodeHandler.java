package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.Pointer;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

@Component
public class ClickNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public ClickNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "CLICK";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        WebElement element = StepTargets.require(node, context, finder);
        if (node.bool("forceJsClick", false)) {
            ((org.openqa.selenium.JavascriptExecutor) context.driver()).executeScript("arguments[0].click();", element);
            return NodeOutput.pass("Clicked");
        }
        return NodeOutput.pass(Pointer.click(context.driver(), element));
    }
}
