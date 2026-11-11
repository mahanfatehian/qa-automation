package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class GoForwardNodeHandler implements NodeHandler {
    @Override
    public String type() {
        return "GO_FORWARD";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        context.driver().navigate().forward();
        new WebDriverWait(context.driver(), Duration.ofSeconds(node.integer("timeoutSeconds", 15))).until(driver -> {
            Object state = ((JavascriptExecutor) driver).executeScript("return document.readyState");
            return "complete".equals(state) || "interactive".equals(state);
        });
        String url = context.driver().getCurrentUrl();
        NodeOutput output = NodeOutput.pass(url);
        output.pins.put("text_out", url);
        return output;
    }
}
