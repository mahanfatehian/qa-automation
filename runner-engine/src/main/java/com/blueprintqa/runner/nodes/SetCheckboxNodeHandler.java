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
public class SetCheckboxNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public SetCheckboxNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "SET_CHECKBOX";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        WebElement element = StepTargets.require(node, context, finder);
        boolean want = node.bool("checked", true);
        boolean landed = Pointer.check(context.driver(), element, want, context);
        NodeOutput output = NodeOutput.pass(want ? "Checked" : "Unchecked");
        output.pins.put("bool_out", landed);
        return output;
    }
}
