package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.springframework.stereotype.Component;

@Component
public class ScreenshotNodeHandler implements NodeHandler {
    @Override
    public String type() {
        return "SCREENSHOT";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String label = context.interpolate(node.str("label", "Screenshot"));
        return NodeOutput.pass(label);
    }
}
