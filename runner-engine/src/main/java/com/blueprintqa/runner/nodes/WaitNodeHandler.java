package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class WaitNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public WaitNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "WAIT";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) throws Exception {
        String waitType = node.str("waitType", "FIXED_MS");
        String value = context.interpolate(node.str("value", "1000"));
        int timeout = Math.max(1, node.integer("timeoutSeconds", 15));
        if ("ELEMENT_VISIBLE".equals(waitType) || "ELEMENT_CLICKABLE".equals(waitType)) {
            String query = context.interpolate(node.str("xpath", value == null ? "" : value));
            var element = finder.find(context.driver(), node.str("strategy", "XPATH"), query, timeout);
            new WebDriverWait(context.driver(), Duration.ofSeconds(timeout)).until(
                    "ELEMENT_CLICKABLE".equals(waitType)
                            ? ExpectedConditions.elementToBeClickable(element)
                            : ExpectedConditions.visibilityOf(element)
            );
            return NodeOutput.pass("Element ready");
        }
        if ("URL_CONTAINS".equals(waitType)) {
            new WebDriverWait(context.driver(), Duration.ofSeconds(timeout)).until(ExpectedConditions.urlContains(value == null ? "" : value));
            return NodeOutput.pass("URL matched");
        }
        if ("NETWORK_IDLE".equals(waitType)) {
            new WebDriverWait(context.driver(), Duration.ofSeconds(timeout)).until(driver -> Boolean.TRUE.equals(
                    ((JavascriptExecutor) driver).executeScript("""
                            const entries = performance.getEntriesByType('resource');
                            const recent = entries.filter(e => e.responseEnd > 0 && (performance.now() - e.responseEnd) < 500);
                            return document.readyState === 'complete' && recent.length === 0;
                            """)
            ));
            return NodeOutput.pass("Page settled");
        }
        long millis = 1000L;
        try {
            millis = Long.parseLong(value == null ? "1000" : value.trim());
        } catch (NumberFormatException ignored) {
            millis = 1000L;
        }
        millis = Math.max(0L, Math.min(millis, 120_000L));
        Thread.sleep(millis);
        return NodeOutput.pass("Waited " + millis + "ms");
    }
}
