package com.ikanobank.onboarding.orchestration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.temporal")
public record TemporalProperties(boolean enabled, String target, String namespace, String taskQueue) {
}
