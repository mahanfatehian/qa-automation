package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.Pointer;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SwitchTabNodeHandler implements NodeHandler {
    @Override
    public String type() {
        return "SWITCH_TAB";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        WebDriver driver = context.driver();
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        String fragment = context.interpolate(node.str("urlContains", ""));
        if (fragment != null && !fragment.isBlank()) {
            for (String handle : handles) {
                driver.switchTo().window(handle);
                if (driver.getCurrentUrl().contains(fragment)) {
                    return switched(context);
                }
            }
            throw new IllegalStateException("No tab URL contains " + fragment);
        }
        int index = node.integer("index", handles.size() - 1);
        if (index < 0 || index >= handles.size()) {
            throw new IllegalArgumentException("Tab index " + index + " is out of range");
        }
        driver.switchTo().window(handles.get(index));
        return switched(context);
    }

    private NodeOutput switched(ExecutionContext context) {
        Pointer.settle(context.driver());
        context.invalidateElements();
        return NodeOutput.pass(context.driver().getCurrentUrl());
    }
}
