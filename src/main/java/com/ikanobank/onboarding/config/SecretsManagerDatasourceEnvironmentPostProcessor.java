package com.ikanobank.onboarding.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public class SecretsManagerDatasourceEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String PROPERTY_SOURCE_NAME = "awsSecretsManagerDatasource";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.acceptsProfiles("test")) {
            return;
        }

        String secretId = environment.getProperty("IKANOBANK_DB_SECRET_ID");
        if (secretId == null || secretId.isBlank()) {
            return;
        }

        String region = environment.getProperty("AWS_REGION", "eu-north-1");
        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(region))
                .build()) {
            String secretString = client.getSecretValue(GetSecretValueRequest.builder()
                            .secretId(secretId)
                            .build())
                    .secretString();

            Map<String, Object> datasourceProperties = datasourceProperties(secretString);
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, datasourceProperties));
        }
    }

    private Map<String, Object> datasourceProperties(String secretString) {
        Map<String, Object> properties = new HashMap<>();
        try {
            Map<String, Object> secret = new ObjectMapper().readValue(secretString, new TypeReference<>() {});
            putIfPresent(properties, "spring.datasource.url", secret.get("url"));
            putIfPresent(properties, "spring.datasource.username", secret.get("username"));
            putIfPresent(properties, "spring.datasource.password", secret.get("password"));
        } catch (Exception ignored) {
            properties.put("spring.datasource.password", secretString);
        }
        return properties;
    }

    private void putIfPresent(Map<String, Object> properties, String key, Object value) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            properties.put(key, stringValue);
        }
    }
}
