package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ConditionNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public ConditionNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "IF_CONDITION";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        boolean passed = "ELEMENT_EXISTS".equals(node.str("conditionType", "TEXT_CONTAINS"))
                ? elementExists(node, context)
                : textMatches(node, context);
        NodeOutput output = NodeOutput.pass(passed ? "True" : "False");
        output.pins.put("bool_out", passed);
        output.nextExec = passed ? "exec_true" : "exec_false";
        return output;
    }

    private boolean elementExists(GraphNode node, ExecutionContext context) {
        String query = context.interpolate(node.str("xpath", node.str("query", "")));
        if (query == null || query.isBlank()) {
            return false;
        }
        try {
            finder.find(context.driver(), node.str("strategy", "XPATH"), query, node.integer("timeoutSeconds", 5));
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean textMatches(GraphNode node, ExecutionContext context) {
        String left = context.textInput();
        if (left == null) {
            String variable = node.str("leftVariable", "");
            if (!variable.isBlank() && context.variable(variable) != null) {
                left = String.valueOf(context.variable(variable));
            }
        }
        if (left == null) {
            left = context.interpolate(node.str("leftValue", ""));
        }
        String right = context.interpolate(node.str("rightValue", ""));
        left = left == null ? "" : left;
        right = right == null ? "" : right;
        return switch (node.str("conditionType", "TEXT_CONTAINS")) {
            case "TEXT_EQUALS" -> left.equals(right);
            case "REGEX_MATCH" -> Pattern.compile(right).matcher(left).find();
            default -> left.contains(right);
        };
    }
}
