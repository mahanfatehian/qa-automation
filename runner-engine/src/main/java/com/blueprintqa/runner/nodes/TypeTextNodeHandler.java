package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
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
        WebElement element = context.elementInput();
        if (element == null) {
            String query = context.interpolate(node.str("xpath", node.str("query", "")));
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("Type needs an element pin or an XPath");
            }
            element = finder.find(context.driver(), node.str("strategy", "XPATH"), query, node.integer("timeoutSeconds", 10));
        }
        String text = context.textInput();
        if (text == null) {
            text = context.interpolate(node.str("text", ""));
        }
        if (text == null) {
            text = "";
        }
        ((JavascriptExecutor) context.driver()).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
        if (node.bool("clearFirst", true)) {
            element.clear();
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
