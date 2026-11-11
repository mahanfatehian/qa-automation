package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;

public interface NodeHandler {
    String type();

    NodeOutput execute(GraphNode node, ExecutionContext context) throws Exception;
}
