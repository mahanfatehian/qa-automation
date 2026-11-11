package com.blueprintqa.runner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    @Value("${blueprintqa.queue}")
    private String queue;

    @Value("${blueprintqa.telemetry-channel}")
    private String telemetryChannel;

    public String queue() {
        return queue;
    }

    public String telemetryChannel() {
        return telemetryChannel;
    }
}
