package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.stereotype.Component;

@Component
public class ScrollNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public ScrollNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "SCROLL";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        JavascriptExecutor js = (JavascriptExecutor) context.driver();
        String mode = node.str("mode", "PIXELS").toUpperCase();
        if ("TOP".equals(mode)) {
            js.executeScript("window.scrollTo(0, 0)");
            return NodeOutput.pass("Scrolled to top");
        }
        if ("BOTTOM".equals(mode)) {
            js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
            return NodeOutput.pass("Scrolled to bottom");
        }
        if ("ELEMENT".equals(mode)) {
            js.executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'})", StepTargets.require(node, context, finder));
            return NodeOutput.pass("Scrolled to element");
        }
        int pixels = node.integer("pixels", 600);
        int x = 0;
        int y = 0;
        switch (node.str("direction", "down")) {
            case "up" -> y = -pixels;
            case "left" -> x = -pixels;
            case "right" -> x = pixels;
            default -> y = pixels;
        }
        js.executeScript("window.scrollBy(arguments[0], arguments[1])", x, y);
        return NodeOutput.pass("Scrolled " + pixels + "px");
    }
}
