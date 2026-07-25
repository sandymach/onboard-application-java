package com.ikanobank.onboarding.integration;

public class LocalFileMockClient implements MockIntegrationClient {
    private final String type;
    private final LocalScenarioRepository repository;

    public LocalFileMockClient(String type, LocalScenarioRepository repository) {
        this.type = type;
        this.repository = repository;
    }

    public String type() {
        return type;
    }

    public MockIntegrationResult execute(String scenarioKey) {
        return repository.load(type, scenarioKey);
    }
}
