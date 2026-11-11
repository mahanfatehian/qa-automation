package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

@Component
public class TypeTextNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public TypeTextNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "TYPE_TEXT";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        WebElement element = StepTargets.require(node, context, finder);
        String text = context.textInput();
        if (text == null) {
            text = context.interpolate(node.str("text", ""));
        }
        if (text == null) {
            text = "";
        }
        JavascriptExecutor js = (JavascriptExecutor) context.driver();
        js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].focus();", element);
        if (node.bool("clearFirst", true)) {
            try {
                element.clear();
            } catch (Exception ex) {
                element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            }
        }
        element.sendKeys(text);
        if (node.bool("pressEnter", false)) {
            element.sendKeys(Keys.ENTER);
        }
        NodeOutput output = NodeOutput.pass("Typed");
        output.pins.put("text_out", text);
        return output;
    }
}
