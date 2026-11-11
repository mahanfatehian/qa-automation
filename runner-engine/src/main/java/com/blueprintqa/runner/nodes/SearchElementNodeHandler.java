package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
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
        String strategy = node.str("strategy", "XPATH");
        String query = context.interpolate(node.str("query", ""));
        int timeout = node.integer("timeoutSeconds", 10);
        WebElement element = finder.find(context.driver(), strategy, query, timeout);
        NodeOutput output = NodeOutput.pass("Element found");
        output.pins.put("element_out", element);
        return output;
    }
}
