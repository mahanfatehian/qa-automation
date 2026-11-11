package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class NavigateNodeHandler implements NodeHandler {
    @Override
    public String type() {
        return "NAVIGATE";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String url = context.textInput();
        if (url == null || url.isBlank()) {
            url = context.interpolate(node.str("url", ""));
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Open page needs a URL");
        }
        int timeout = Math.max(5, node.integer("timeoutSeconds", 30));
        context.driver().manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeout));
        context.driver().navigate().to(url);
        new WebDriverWait(context.driver(), Duration.ofSeconds(timeout)).until(driver ->
                "complete".equals(((JavascriptExecutor) driver).executeScript("return document.readyState"))
        );
        String current = context.driver().getCurrentUrl();
        NodeOutput output = NodeOutput.pass(current);
        output.pins.put("text_out", current);
        return output;
    }
}
