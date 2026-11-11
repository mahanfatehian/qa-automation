package com.blueprintqa.runner.engine;

import com.blueprintqa.runner.model.GraphNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class InspectBrowserPool {
    private final SeleniumFactory selenium;
    private final GridBudget budget;
    private final AtomicReference<WebDriver> warm = new AtomicReference<>();
    private volatile String warmBrowser = "chrome";
    private volatile boolean holdingSlot;

    public InspectBrowserPool(SeleniumFactory selenium, GridBudget budget) {
        this.selenium = selenium;
        this.budget = budget;
    }

    @PostConstruct
    void prewarm() {
        Thread.ofVirtual().name("inspect-warm").start(() -> {
            GraphNode start = new GraphNode();
            start.data.put("browser", "chrome");
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    park(take(start));
                    return;
                } catch (Exception ex) {
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
    }

    public WebDriver take(GraphNode start) {
        String browser = start == null ? "chrome" : start.str("browser", "chrome");
        WebDriver ready = warm.getAndSet(null);
        if (ready != null && browser.equalsIgnoreCase(warmBrowser) && alive(ready)) {
            reset(ready);
            size(ready, start);
            return ready;
        }
        if (ready != null) {
            quit(ready);
            releaseSlot();
        }
        budget.acquire();
        holdingSlot = true;
        try {
            WebDriver driver = selenium.open(start, "inspect", 2);
            warmBrowser = browser;
            size(driver, start);
            return driver;
        } catch (RuntimeException ex) {
            releaseSlot();
            throw ex;
        }
    }

    public void park(WebDriver driver) {
        if (driver == null) {
            return;
        }
        reset(driver);
        if (!alive(driver)) {
            quit(driver);
            releaseSlot();
            return;
        }
        WebDriver previous = warm.getAndSet(driver);
        if (previous != null && previous != driver) {
            quit(previous);
        }
    }

    public void discard(WebDriver driver) {
        quit(driver);
        releaseSlot();
    }

    @PreDestroy
    public void shutdown() {
        quit(warm.getAndSet(null));
        releaseSlot();
    }

    private void releaseSlot() {
        if (holdingSlot) {
            holdingSlot = false;
            budget.release();
        }
    }

    private void reset(WebDriver driver) {
        try {
            driver.switchTo().defaultContent();
        } catch (Exception ignored) {
            // frame may already be gone
        }
        try {
            driver.manage().deleteAllCookies();
        } catch (Exception ignored) {
            // blank page has no cookie jar yet
        }
        try {
            driver.get("about:blank");
        } catch (Exception ignored) {
            // the next navigate will replace it
        }
    }

    private void size(WebDriver driver, GraphNode start) {
        int width = start == null ? 1280 : Math.max(320, start.integer("windowWidth", 1280));
        int height = start == null ? 800 : Math.max(240, start.integer("windowHeight", 800));
        try {
            driver.manage().window().setSize(new Dimension(width, height));
        } catch (Exception ignored) {
            // grid may ignore window size
        }
    }

    private boolean alive(WebDriver driver) {
        try {
            driver.getWindowHandle();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void quit(WebDriver driver) {
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } catch (Exception ignored) {
            // already closed
        }
    }
}
