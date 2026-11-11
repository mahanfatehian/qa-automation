package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertNodeHandler implements NodeHandler {
    @Override
    public String type() {
        return "ALERT";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        Alert alert = context.driver().switchTo().alert();
        String text = alert.getText() == null ? "" : alert.getText();
        if ("DISMISS".equalsIgnoreCase(node.str("action", "ACCEPT"))) {
            alert.dismiss();
        } else {
            alert.accept();
        }
        NodeOutput output = NodeOutput.pass(text);
        output.pins.put("text_out", text);
        return output;
    }
}
