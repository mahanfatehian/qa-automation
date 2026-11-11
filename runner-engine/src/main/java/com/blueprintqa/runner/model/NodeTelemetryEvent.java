package com.blueprintqa.runner.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class NodeTelemetryEvent {
    public String runId;
    public String workflowId;
    public String nodeId;
    public String nodeType;
    public String eventType;
    public String timestamp;
    public long durationMs;
    public String message = "";
    public String errorStack = "";
    public Map<String, Object> outputData = new LinkedHashMap<>();
    public String screenshotBase64 = "";
}
