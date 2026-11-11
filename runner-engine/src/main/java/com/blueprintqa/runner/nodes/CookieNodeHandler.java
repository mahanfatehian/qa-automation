package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.Cookie;
import org.springframework.stereotype.Component;

@Component
public class CookieNodeHandler implements NodeHandler {
    @Override
    public String type() {
        return "COOKIE";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String action = node.str("action", "GET").toUpperCase();
        String name = context.interpolate(node.str("cookieName", ""));
        if ("DELETE_ALL".equals(action)) {
            context.driver().manage().deleteAllCookies();
            return NodeOutput.pass("Deleted all cookies");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Cookie needs a name");
        }
        if ("DELETE".equals(action)) {
            context.driver().manage().deleteCookieNamed(name);
            return NodeOutput.pass("Deleted " + name);
        }
        if ("SET".equals(action)) {
            String value = context.textInput();
            if (value == null) {
                value = context.interpolate(node.str("cookieValue", ""));
            }
            context.driver().manage().addCookie(new Cookie(name, value == null ? "" : value));
            NodeOutput output = NodeOutput.pass("Set " + name);
            output.pins.put("text_out", value == null ? "" : value);
            return output;
        }
        Cookie cookie = context.driver().manage().getCookieNamed(name);
        String value = cookie == null ? "" : cookie.getValue();
        NodeOutput output = NodeOutput.pass(value);
        output.pins.put("text_out", value);
        return output;
    }
}
