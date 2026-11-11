package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class UploadNodeHandler implements NodeHandler {
    private final ElementFinder finder;

    public UploadNodeHandler(ElementFinder finder) {
        this.finder = finder;
    }

    @Override
    public String type() {
        return "UPLOAD";
    }

    @Override
    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String path = context.interpolate(node.str("filePath", "/app/fixtures/sample.txt"));
        if (!Files.isRegularFile(Path.of(path))) {
            throw new IllegalArgumentException("Upload file is not in the runner container: " + path);
        }
        StepTargets.require(node, context, finder).sendKeys(path);
        return NodeOutput.pass(path);
    }
}
