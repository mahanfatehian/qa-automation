package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.Pointer;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.springframework.stereotype.Component;

@Component
public class SelectOptionNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public SelectOptionNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "SELECT_OPTION";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        WebElement element = StepTargets.require(node, context, finder);
        String choice = context.textInput();
        if (choice == null || choice.isBlank()) {
            choice = context.interpolate(node.str("option", ""));
        }
        if (choice == null || choice.isBlank()) {
            throw new IllegalArgumentException("Select needs an option");
        }
        Pointer.choose(element, choice, "VALUE".equalsIgnoreCase(node.str("by", "TEXT")));
        NodeOutput output = NodeOutput.pass("Selected " + choice);
        output.pins.put("text_out", choice);
        return output;
    }
}
