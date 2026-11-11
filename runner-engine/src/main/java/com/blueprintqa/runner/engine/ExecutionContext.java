package com.blueprintqa.runner.engine;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecutionContext {
    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");

    private final WebDriver driver;
    private final Map<String, Object> variables;
    private final Map<String, Map<String, Object>> pins = new HashMap<>();
    private final Map<String, Object> inputs = new HashMap<>();
    private String branch = "";

    public ExecutionContext(WebDriver driver, Map<String, Object> variables) {
        this.driver = driver;
        this.variables = variables == null ? new HashMap<>() : variables;
    }

    public WebDriver driver() {
        return driver;
    }

    public String branch() {
        return branch;
    }

    public synchronized ExecutionContext fork(WebDriver next, String branchName) {
        ExecutionContext copy = new ExecutionContext(next, new HashMap<>(variables));
        copy.branch = branchName == null ? "" : branchName;
        for (Map.Entry<String, Map<String, Object>> nodePins : pins.entrySet()) {
            for (Map.Entry<String, Object> pin : nodePins.getValue().entrySet()) {
                if (pin.getValue() instanceof WebElement) {
                    continue;
                }
                copy.setPin(nodePins.getKey(), pin.getKey(), pin.getValue());
            }
        }
        return copy;
    }

    public void clearInputs() {
        inputs.clear();
    }

    public void putInput(String handle, Object value) {
        if (handle != null && value != null) {
            inputs.put(handle, value);
        }
    }

    public Object input(String handle) {
        return inputs.get(handle);
    }

    public WebElement elementInput() {
        Object direct = inputs.get("element_in");
        if (direct instanceof WebElement element) {
            return element;
        }
        Object current = inputs.get("current_element");
        if (current instanceof WebElement element) {
            return element;
        }
        return null;
    }

    public ElementRef elementRef() {
        Object direct = inputs.get("element_in_ref");
        if (direct instanceof ElementRef ref) {
            return ref;
        }
        Object current = inputs.get("current_element_ref");
        if (current instanceof ElementRef ref) {
            return ref;
        }
        return null;
    }

    public String textInput() {
        Object value = inputs.get("text_in");
        if (value == null) {
            value = inputs.get("extracted_value");
        }
        return value == null ? null : String.valueOf(value);
    }

    public void setPin(String nodeId, String handle, Object value) {
        pins.computeIfAbsent(nodeId, ignored -> new HashMap<>()).put(handle, value);
    }

    public Object getPin(String nodeId, String handle) {
        Map<String, Object> nodePins = pins.get(nodeId);
        if (nodePins == null) {
            return null;
        }
        return nodePins.get(handle);
    }

    public void setVariable(String name, Object value) {
        if (name == null || name.isBlank() || value == null || value instanceof WebElement) {
            return;
        }
        variables.put(name, value);
    }

    public Object variable(String name) {
        return variables.get(name);
    }

    public String interpolate(String raw) {
        if (raw == null || raw.isBlank() || !raw.contains("{{")) {
            return raw;
        }
        Matcher matcher = TOKEN.matcher(raw);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            Object value = variables.get(matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
