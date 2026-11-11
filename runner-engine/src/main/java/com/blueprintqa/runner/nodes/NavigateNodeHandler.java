package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.Pointer;
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
        if (Boolean.TRUE.equals(context.variable("__inspect"))) {
            timeout = Math.min(timeout, 12);
        }
        context.driver().manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeout));
        Exception last = null;
        int tries = Boolean.TRUE.equals(context.variable("__inspect")) ? 1 : 2;
        for (int attempt = 1; attempt <= tries; attempt++) {
            try {
                context.driver().navigate().to(url);
                new WebDriverWait(context.driver(), Duration.ofSeconds(timeout)).until(driver ->
                        "complete".equals(((JavascriptExecutor) driver).executeScript("return document.readyState"))
                );
                last = null;
                break;
            } catch (Exception ex) {
                last = ex;
                if (attempt == tries) {
                    throw new IllegalStateException("Open page failed for " + url + ": " + ex.getMessage(), ex);
                }
            }
        }
        if (last != null) {
            throw new IllegalStateException("Open page failed for " + url, last);
        }
        Pointer.settle(context.driver());
        String current = context.driver().getCurrentUrl();
        if (current == null || current.isBlank() || current.startsWith("data:") || current.startsWith("about:")) {
            throw new IllegalStateException("Open page did not land. The browser is still on a blank page.");
        }
        context.invalidateElements();
        NodeOutput output = NodeOutput.pass(current);
        output.pins.put("text_out", current);
        return output;
    }
}
