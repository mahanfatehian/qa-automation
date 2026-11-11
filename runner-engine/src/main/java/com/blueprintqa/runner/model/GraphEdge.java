package com.blueprintqa.runner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphEdge {
    public String id;
    public String source;
    public String target;
    public String sourceHandle;
    public String targetHandle;

    public String sourceHandleOrDefault() {
        return sourceHandle == null || sourceHandle.isBlank() ? "exec_out" : sourceHandle;
    }

    public String targetHandleOrDefault() {
        return targetHandle == null || targetHandle.isBlank() ? "exec_in" : targetHandle;
    }
}
