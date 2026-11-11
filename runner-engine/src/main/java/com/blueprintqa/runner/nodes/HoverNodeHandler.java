package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.Pointer;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.springframework.stereotype.Component;

@Component
public class HoverNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public HoverNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "HOVER";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        var element = StepTargets.require(node, context, finder);
        Pointer.hover(context.driver(), element);
        Object covered = ((org.openqa.selenium.JavascriptExecutor) context.driver()).executeScript("""
                const el = arguments[0];
                const box = el.getBoundingClientRect();
                const top = document.elementFromPoint(box.left + box.width / 2, box.top + box.height / 2);
                return !(top === el || (top && (el.contains(top) || top.contains(el))));
                """, element);
        if (Boolean.TRUE.equals(covered)) {
            throw new IllegalStateException("Hover did not land. Another element is covering this one.");
        }
        return NodeOutput.pass("Hovered");
    }
}
