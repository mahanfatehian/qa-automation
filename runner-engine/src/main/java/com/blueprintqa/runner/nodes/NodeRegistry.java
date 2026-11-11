package com.blueprintqa.runner.nodes;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NodeRegistry {
    private final Map<String, NodeHandler> handlers;

    public NodeRegistry(List<NodeHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toMap(NodeHandler::type, handler -> handler));
    }

    public NodeHandler get(String type) {
        NodeHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown blueprint step: " + type);
        }
        return handler;
    }
}
