package com.ikanobank.onboarding.integration;

public interface MockIntegrationClient {
    String type();

    MockIntegrationResult execute(String scenarioKey);
}
