package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExtractContentNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public ExtractContentNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "EXTRACT_CONTENT";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        WebElement element = context.elementInput();
        String query = context.interpolate(node.str("query", node.str("xpath", "")));
        if (element == null && query != null && !query.isBlank()) {
            element = finder.find(context.driver(), node.str("strategy", "XPATH"), query, node.integer("timeoutSeconds", 10));
        }
        String kind = node.str("extractType", "INNER_TEXT");
        String value = switch (kind) {
            case "ATTRIBUTE" -> element == null ? "" : safeAttr(element, node.str("attributeName", "href"));
            case "OUTER_HTML" -> element == null ? "" : safeAttr(element, "outerHTML");
            case "REGEX_MATCH" -> regex(element == null ? context.driver().getPageSource() : element.getText(), node.str("regexPattern", ""));
            default -> element == null ? "" : element.getText();
        };
        if (value == null) {
            value = "";
        }
        String preview = value.length() > 180 ? value.substring(0, 180) + "..." : value;
        NodeOutput output = NodeOutput.pass(preview.isBlank() ? "Read empty text" : preview);
        output.pins.put("text_out", value);
        output.pins.put("extracted_value", value);
        return output;
    }

    private String safeAttr(WebElement element, String name) {
        String value = element.getAttribute(name);
        return value == null ? "" : value;
    }

    private String regex(String source, String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("Read text needs a regex pattern");
        }
        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(source == null ? "" : source);
        if (!matcher.find()) {
            return "";
        }
        return matcher.groupCount() >= 1 ? matcher.group(1) : matcher.group();
    }
}
