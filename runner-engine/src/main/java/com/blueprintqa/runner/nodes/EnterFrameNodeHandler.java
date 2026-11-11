package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.springframework.stereotype.Component;

@Component
public class EnterFrameNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public EnterFrameNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "ENTER_FRAME";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        context.driver().switchTo().frame(StepTargets.require(node, context, finder));
        return NodeOutput.pass("Entered frame");
    }
}
