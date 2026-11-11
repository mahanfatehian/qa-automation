package com.blueprintqa.runner.nodes;

import com.blueprintqa.runner.engine.ElementFinder;
import com.blueprintqa.runner.engine.ExecutionContext;
import com.blueprintqa.runner.engine.Pointer;
import com.blueprintqa.runner.engine.StepTargets;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.NodeOutput;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;

@Component
class OpenTabNodeHandler implements NodeHandler {
    public String type() { return "OPEN_TAB"; }

    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String url = context.textInput();
        if (url == null || url.isBlank()) url = context.interpolate(node.str("url", ""));
        if (url.isBlank()) throw new IllegalArgumentException("Open tab needs a URL");
        ((JavascriptExecutor) context.driver()).executeScript("window.open(arguments[0], '_blank');", url);
        ArrayList<String> handles = new ArrayList<>(context.driver().getWindowHandles());
        context.driver().switchTo().window(handles.get(handles.size() - 1));
        Pointer.settle(context.driver());
        context.invalidateElements();
        NodeOutput output = NodeOutput.pass(context.driver().getCurrentUrl());
        output.pins.put("text_out", output.message);
        return output;
    }
}

@Component
class CloseTabNodeHandler implements NodeHandler {
    public String type() { return "CLOSE_TAB"; }

    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        WebDriver driver = context.driver();
        String current = driver.getWindowHandle();
        ArrayList<String> handles = new ArrayList<>(driver.getWindowHandles());
        if (handles.size() < 2) throw new IllegalStateException("There is no other tab to return to");
        driver.close();
        handles.remove(current);
        driver.switchTo().window(handles.get(handles.size() - 1));
        Pointer.settle(driver);
        context.invalidateElements();
        NodeOutput output = NodeOutput.pass(driver.getCurrentUrl());
        output.pins.put("text_out", output.message);
        return output;
    }
}

@Component
class FocusNodeHandler implements NodeHandler {
    private final ElementFinder finder;
    FocusNodeHandler(ElementFinder finder) { this.finder = finder; }
    public String type() { return "FOCUS"; }

    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        var element = StepTargets.require(node, context, finder);
        ((JavascriptExecutor) context.driver()).executeScript("arguments[0].focus();", element);
        return NodeOutput.pass("Focused");
    }
}

@Component
class ClearFieldNodeHandler implements NodeHandler {
    private final ElementFinder finder;
    ClearFieldNodeHandler(ElementFinder finder) { this.finder = finder; }
    public String type() { return "CLEAR_FIELD"; }

    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        var element = StepTargets.require(node, context, finder);
        element.clear();
        return NodeOutput.pass("Cleared");
    }
}

@Component
class ReadUrlNodeHandler implements NodeHandler {
    public String type() { return "READ_URL"; }

    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String url = context.driver().getCurrentUrl();
        NodeOutput output = NodeOutput.pass(url);
        output.pins.put("text_out", url);
        return output;
    }
}

@Component
class ReadTitleNodeHandler implements NodeHandler {
    public String type() { return "READ_TITLE"; }

    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String title = context.driver().getTitle();
        NodeOutput output = NodeOutput.pass(title);
        output.pins.put("text_out", title);
        return output;
    }
}

@Component
class SetWindowNodeHandler implements NodeHandler {
    public String type() { return "SET_WINDOW"; }

    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        int width = Math.max(320, node.integer("windowWidth", 1280));
        int height = Math.max(240, node.integer("windowHeight", 800));
        context.driver().manage().window().setSize(new org.openqa.selenium.Dimension(width, height));
        return NodeOutput.pass(width + "x" + height);
    }
}

@Component
class MaximizeNodeHandler implements NodeHandler {
    public String type() { return "MAXIMIZE"; }

    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        context.driver().manage().window().maximize();
        return NodeOutput.pass("Maximized");
    }
}

@Component
class CountElementsNodeHandler implements NodeHandler {
    private final ElementFinder finder;
    CountElementsNodeHandler(ElementFinder finder) { this.finder = finder; }
    public String type() { return "COUNT_ELEMENTS"; }

    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String query = context.interpolate(node.str("query", node.str("xpath", "")));
        int count = finder.count(context.driver(), node.str("strategy", "XPATH"), query);
        NodeOutput output = NodeOutput.pass(String.valueOf(count));
        output.pins.put("text_out", String.valueOf(count));
        return output;
    }
}

@Component
class WaitGoneNodeHandler implements NodeHandler {
    public String type() { return "WAIT_GONE"; }

    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String query = context.interpolate(node.str("query", node.str("xpath", "")));
        if (query.isBlank()) throw new IllegalArgumentException("Wait until gone needs a selector");
        int timeout = Math.max(1, node.integer("timeoutSeconds", 10));
        org.openqa.selenium.By by = new ElementFinder().by(node.str("strategy", "XPATH"), query);
        if (by == null) throw new IllegalArgumentException("Wait until gone needs a locator strategy");
        new WebDriverWait(context.driver(), Duration.ofSeconds(timeout)).until(org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated(by));
        return NodeOutput.pass("Gone");
    }
}

@Component
class StorageNodeHandler implements NodeHandler {
    public String type() { return "PAGE_STORAGE"; }

    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        String action = node.str("action", "GET");
        String key = context.interpolate(node.str("storageKey", ""));
        if (key.isBlank()) throw new IllegalArgumentException("Page storage needs a key");
        JavascriptExecutor js = (JavascriptExecutor) context.driver();
        if ("SET".equals(action)) {
            String value = context.textInput();
            if (value == null) value = context.interpolate(node.str("storageValue", ""));
            js.executeScript("localStorage.setItem(arguments[0], arguments[1]);", key, value);
            return NodeOutput.pass("Saved");
        }
        if ("DELETE".equals(action)) {
            js.executeScript("localStorage.removeItem(arguments[0]);", key);
            return NodeOutput.pass("Deleted");
        }
        Object value = js.executeScript("return localStorage.getItem(arguments[0]);", key);
        String text = value == null ? "" : String.valueOf(value);
        NodeOutput output = NodeOutput.pass(text);
        output.pins.put("text_out", text);
        return output;
    }
}

@Component
class SubmitNodeHandler implements NodeHandler {
    private final ElementFinder finder;
    SubmitNodeHandler(ElementFinder finder) { this.finder = finder; }
    public String type() { return "SUBMIT"; }

    public NodeOutput execute(GraphNode node, ExecutionContext context) {
        var element = StepTargets.require(node, context, finder);
        String before = context.driver().getCurrentUrl();
        element.submit();
        Pointer.settle(context.driver());
        if (before.equals(context.driver().getCurrentUrl())) {
            throw new IllegalStateException("Submit did not land. The page did not leave this URL.");
        }
        context.invalidateElements();
        return NodeOutput.pass("Submitted");
    }
}
