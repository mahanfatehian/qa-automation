package com.blueprintqa.runner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphNode {
    public String id;
    public String type;
    public Map<String, Object> data = new HashMap<>();
    public Map<String, Object> position;

    public String semanticType() {
        if (data != null && data.get("nodeType") != null) {
            return String.valueOf(data.get("nodeType"));
        }
        return type == null ? "" : type;
    }

    public String str(String key, String fallback) {
        if (data == null || data.get(key) == null) {
            return fallback;
        }
        return String.valueOf(data.get(key));
    }

    public int integer(String key, int fallback) {
        if (data == null || data.get(key) == null) {
            return fallback;
        }
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public boolean bool(String key, boolean fallback) {
        if (data == null || data.get(key) == null) {
            return fallback;
        }
        Object value = data.get(key);
        if (value instanceof Boolean flag) {
            return flag;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }
}
