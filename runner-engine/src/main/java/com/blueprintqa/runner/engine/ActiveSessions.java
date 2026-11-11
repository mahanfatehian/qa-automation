package com.blueprintqa.runner.engine;

import jakarta.annotation.PreDestroy;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveSessions {
    private final Set<WebDriver> drivers = ConcurrentHashMap.newKeySet();

    public void add(WebDriver driver) {
        if (driver != null) {
            drivers.add(driver);
        }
    }

    public void remove(WebDriver driver) {
        if (driver != null) {
            drivers.remove(driver);
        }
    }

    @PreDestroy
    public void quitAll() {
        for (WebDriver driver : drivers) {
            try {
                driver.quit();
            } catch (Exception ignored) {
                // session may already be closed
            }
        }
        drivers.clear();
    }
}
