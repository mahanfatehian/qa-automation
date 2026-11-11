package com.blueprintqa.runner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowJob {
    public String runId;
    public String workflowId;
    public String workflowName;
    public int maxSteps = 500;
    public List<GraphNode> nodes = new ArrayList<>();
    public List<GraphEdge> edges = new ArrayList<>();
    public Map<String, Object> variables = new HashMap<>();

    public GraphNode node(String id) {
        if (nodes == null) {
            return null;
        }
        for (GraphNode node : nodes) {
            if (node != null && id.equals(node.id)) {
                return node;
            }
        }
        return null;
    }

    public GraphNode start() {
        if (nodes != null) {
            for (GraphNode node : nodes) {
                if (node != null && "START".equals(node.semanticType())) {
                    return node;
                }
            }
        }
        throw new IllegalStateException("Blueprint has no Start browser step");
    }
}
