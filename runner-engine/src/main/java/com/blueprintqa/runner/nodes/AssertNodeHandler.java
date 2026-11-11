package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class AssertNodeHandler implements NodeHandler {
    @Override
    public String type() {
        return "ASSERT";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String actual = context.textInput();
        if (actual == null) {
            String variable = node.str("actualVariable", "");
            if (!variable.isBlank() && context.variable(variable) != null) {
                actual = String.valueOf(context.variable(variable));
            }
        }
        if (actual == null) {
            actual = context.interpolate(node.str("actual", ""));
        }
        String operator = node.str("operator", "NOT_EMPTY");
        String expected = context.interpolate(node.str("expected", ""));
        boolean passed = switch (operator) {
            case "EQUALS" -> actual != null && actual.equals(expected);
            case "CONTAINS" -> actual != null && actual.contains(expected == null ? "" : expected);
            case "MATCHES_REGEX" -> actual != null && Pattern.compile(expected == null ? "" : expected).matcher(actual).find();
            default -> actual != null && !actual.isBlank();
        };
        if (!passed) {
            throw new AssertionError("Check failed: " + operator + " expected [" + expected + "] but was [" + actual + "]");
        }
        NodeOutput output = NodeOutput.pass("Check passed");
        output.pins.put("text_out", actual == null ? "" : actual);
        return output;
    }
}
