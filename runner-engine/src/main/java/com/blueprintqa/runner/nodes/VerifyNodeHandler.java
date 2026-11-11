package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

@Component
public class VerifyNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public VerifyNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "VERIFY";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String check = node.str("check", "VISIBLE").toUpperCase();
        String expected = context.interpolate(node.str("expected", ""));
        String actual;
        boolean passed = switch (check) {
            case "ENABLED" -> {
                actual = String.valueOf(StepTargets.require(node, context, finder).isEnabled());
                yield "true".equals(actual);
            }
            case "SELECTED" -> {
                actual = String.valueOf(StepTargets.require(node, context, finder).isSelected());
                yield "true".equals(actual);
            }
            case "ATTRIBUTE" -> {
                WebElement element = StepTargets.require(node, context, finder);
                String value = element.getAttribute(node.str("attributeName", "href"));
                actual = value == null ? "" : value;
                yield actual.equals(expected);
            }
            case "COUNT" -> {
                int count = finder.count(context.driver(), node.str("strategy", "XPATH"), context.interpolate(node.str("xpath", node.str("query", ""))));
                actual = String.valueOf(count);
                yield actual.equals(expected);
            }
            case "URL_EQUALS" -> {
                actual = context.driver().getCurrentUrl();
                yield actual.equals(expected);
            }
            case "URL_CONTAINS" -> {
                actual = context.driver().getCurrentUrl();
                yield actual.contains(expected);
            }
            case "TITLE_CONTAINS" -> {
                actual = context.driver().getTitle();
                yield actual != null && actual.contains(expected);
            }
            case "TEXT_EQUALS" -> {
                actual = textOf(node, context);
                yield actual.equals(expected);
            }
            case "TEXT_CONTAINS" -> {
                actual = textOf(node, context);
                yield actual.contains(expected);
            }
            default -> {
                WebElement element = StepTargets.require(node, context, finder);
                actual = String.valueOf(element.isDisplayed());
                yield element.isDisplayed();
            }
        };
        if (!passed) {
            throw new AssertionError("Verify failed: " + check + " expected [" + expected + "] but was [" + actual + "]");
        }
        NodeOutput output = NodeOutput.pass(check + " passed: " + actual);
        output.pins.put("bool_out", true);
        output.pins.put("text_out", actual);
        return output;
    }

    private String textOf(GraphNode node, ExecutionContext context) {
        String pinned = context.textInput();
        if (pinned != null) {
            return pinned;
        }
        return StepTargets.require(node, context, finder).getText();
    }
}
