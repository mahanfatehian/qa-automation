package com.blueprintqa.runner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeleniumConfig {
    @Value("${blueprintqa.selenium-grid-url}")
    private String gridUrl;

    public String gridUrl() {
        return gridUrl;
    }
}
