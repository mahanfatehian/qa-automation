package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.springframework.stereotype.Component;

@Component
public class StartNodeHandler implements NodeHandler {
    @Override
    public String type() {
        return "START";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        NodeOutput output = NodeOutput.pass("Browser ready");
        output.pins.put("text_out", node.str("browser", "chrome"));
        return output;
    }
}
