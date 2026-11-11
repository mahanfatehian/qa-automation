package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ElementRef;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

@Component
public class SearchElementNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public SearchElementNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "SEARCH_ELEMENT";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        WebElement element = StepTargets.find(node, context, finder, "Find needs a selector. Use Select on page, or type a CSS, XPath, or text query.");
        NodeOutput output = NodeOutput.pass("Element found");
        output.pins.put("element_out", element);
        output.pins.put("element_out_ref", ElementRef.from(node, context));
        return output;
    }
}
