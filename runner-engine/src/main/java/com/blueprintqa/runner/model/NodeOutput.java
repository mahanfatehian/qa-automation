package com.blueprintqa.runner.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class NodeOutput {
    public Map<String, Object> pins = new LinkedHashMap<>();
    public String nextExec = "exec_out";
    public String message = "";

    public static NodeOutput pass(String message) {
        NodeOutput output = new NodeOutput();
        output.message = message == null ? "" : message;
        return output;
    }
}
