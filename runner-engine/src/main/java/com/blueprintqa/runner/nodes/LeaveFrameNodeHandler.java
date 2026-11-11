package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.springframework.stereotype.Component;

@Component
public class LeaveFrameNodeHandler implements NodeHandler {
    @Override
    public String type() {
        return "LEAVE_FRAME";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        context.driver().switchTo().defaultContent();
        return NodeOutput.pass("Left frame");
    }
}
