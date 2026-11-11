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
public class RefreshNodeHandler implements NodeHandler {
    @Override
    public String type() {
        return "REFRESH";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        context.driver().navigate().refresh();
        new WebDriverWait(context.driver(), Duration.ofSeconds(node.integer("timeoutSeconds", 15))).until(driver ->
                "complete".equals(((JavascriptExecutor) driver).executeScript("return document.readyState"))
                || "interactive".equals(((JavascriptExecutor) driver).executeScript("return document.readyState"))
        );
        Pointer.settle(context.driver());
        context.invalidateElements();
        String url = context.driver().getCurrentUrl();
        NodeOutput output = NodeOutput.pass(url);
        output.pins.put("text_out", url);
        return output;
    }
}
