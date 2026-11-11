package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.springframework.stereotype.Component;

@Component
public class PressKeyNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public PressKeyNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "PRESS_KEY";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String name = context.interpolate(node.str("key", "ENTER"));
        Keys key = Keys.valueOf(name.trim().toUpperCase().replace(' ', '_'));
        if (context.elementInput() != null || !node.str("xpath", node.str("query", "")).isBlank()) {
            WebElement element = StepTargets.require(node, context, finder);
            element.sendKeys(key);
        } else {
            new Actions(context.driver()).sendKeys(key).perform();
        }
        return NodeOutput.pass("Pressed " + key.name());
    }
}
