package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
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
        JavascriptExecutor js = (JavascriptExecutor) context.driver();
        js.executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", element);
        if (node.bool("forceJsClick", false)) {
            js.executeScript("arguments[0].click();", element);
        } else {
            try {
                element.click();
            } catch (ElementNotInteractableException ex) {
                js.executeScript("arguments[0].click();", element);
            }
        }
        return NodeOutput.pass("Clicked");
    }
}
