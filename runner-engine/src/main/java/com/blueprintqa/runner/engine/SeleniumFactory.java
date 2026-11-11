package com.blueprintqa.runner.engine;

import com.blueprintqa.runner.config.SeleniumConfig;
import com.blueprintqa.runner.model.GraphNode;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;

@Component
public class SeleniumFactory {
    private static final Logger log = LoggerFactory.getLogger(SeleniumFactory.class);
    private final SeleniumConfig config;

    public SeleniumFactory(SeleniumConfig config) {
        this.config = config;
    }

    public WebDriver open(GraphNode start, String sessionName) {
        return open(start, sessionName, 8);
    }

    public WebDriver open(GraphNode start, String sessionName, int attempts) {
        String browser = start == null ? "chrome" : start.str("browser", "chrome");
        boolean headless = start == null || start.bool("headless", true);
        int width = start == null ? 1280 : Math.max(320, start.integer("windowWidth", 1280));
        int height = start == null ? 800 : Math.max(240, start.integer("windowHeight", 800));
        Exception last = null;
        int tries = Math.max(1, attempts);
        for (int attempt = 1; attempt <= tries; attempt++) {
            try {
                WebDriver driver = connect(browser, headless, width, height, sessionName);
                driver.manage().timeouts().implicitlyWait(Duration.ZERO);
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
                driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
                try {
                    driver.manage().window().setSize(new Dimension(width, height));
                } catch (Exception ignored) {
                    log.debug("grid ignored window size");
                }
                return driver;
            } catch (Exception ex) {
                last = ex;
                log.warn("grid session attempt {}/{} failed: {}", attempt, tries, ex.getMessage());
                sleep(1000L * attempt);
            }
        }
        throw new IllegalStateException("Could not open a browser on the Selenium Grid. Check that a matching node is registered.", last);
    }

    private WebDriver connect(String browser, boolean headless, int width, int height, String sessionName) throws Exception {
        URI address = URI.create(normalize(config.gridUrl()));
        if ("firefox".equalsIgnoreCase(browser)) {
            FirefoxOptions options = new FirefoxOptions();
            if (headless) {
                options.addArguments("-headless");
            }
            options.addArguments("--width=" + width, "--height=" + height);
            if (sessionName != null && !sessionName.isBlank()) {
                options.setCapability("se:name", sessionName);
            }
            return new RemoteWebDriver(address.toURL(), options);
        }
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--disable-extensions", "--window-size=" + width + "," + height);
        options.setAcceptInsecureCerts(true);
        if (sessionName != null && !sessionName.isBlank()) {
            options.setCapability("se:name", sessionName);
        }
        return new RemoteWebDriver(address.toURL(), options);
    }

    private String normalize(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.isBlank()) {
            url = "http://selenium-hub:4444";
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
