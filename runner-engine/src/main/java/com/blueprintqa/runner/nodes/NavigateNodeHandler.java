package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.Pointer;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
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
                last = null;
                break;
            } catch (Exception ex) {
                last = ex;
                String landed = readUrl(context);
                if (isPage(landed)) {
                    last = null;
                    break;
                }
            }
        }
        if (last != null) {
            throw new IllegalStateException("Open page failed for " + url, last);
        }
        try {
            Pointer.settle(context.driver());
        } catch (RuntimeException ignored) {
            // a heavy page can stop answering while it paints; the URL check below is the gate
        }
        String current = readUrl(context);
        if (!isPage(current)) {
            throw new IllegalStateException("Open page did not land. The browser is still on a blank page.");
        }
        context.invalidateElements();
        NodeOutput output = NodeOutput.pass(current);
        output.pins.put("text_out", current);
        return output;
    }

    private static boolean isPage(String url) {
        return url != null && !url.isBlank() && !url.startsWith("about:") && !url.startsWith("data:");
    }

    private static String readUrl(ExecutionContext context) {
        for (int attempt = 0; attempt < 8; attempt++) {
            try {
                return context.driver().getCurrentUrl();
            } catch (RuntimeException ex) {
                try {
                    Thread.sleep(400L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return "";
                }
            }
        }
        return "";
    }
}
