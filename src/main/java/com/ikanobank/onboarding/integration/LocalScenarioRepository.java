package com.ikanobank.onboarding.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class LocalScenarioRepository {
    private static final Logger log = LoggerFactory.getLogger(LocalScenarioRepository.class);

    private final ObjectMapper mapper;
    private final Path mockDataPath;

    public LocalScenarioRepository(ObjectMapper mapper,
                                   @Value("${app.mock-data.path:mock-data}") String mockDataPath) {
        this.mapper = mapper;
        this.mockDataPath = resolveBasePath(mockDataPath);
    }

    public MockIntegrationResult load(String integrationType, String scenarioKey) {
        Path specific = mockDataPath.resolve(integrationType).resolve(scenarioKey + ".json");
        if (Files.exists(specific)) {
            return read(specific);
        }

        MockIntegrationResult classpathSpecific = readClasspath(integrationType, scenarioKey + ".json");
        if (classpathSpecific != null) {
            return classpathSpecific;
        }

        log.warn("Local mock scenario not found path={}, using default", specific);
        Path fallback = mockDataPath.resolve(integrationType).resolve("default.json");
        if (Files.exists(fallback)) {
            return read(fallback);
        }

        MockIntegrationResult classpathDefault = readClasspath(integrationType, "default.json");
        if (classpathDefault != null) {
            return classpathDefault;
        }

        throw new IllegalStateException("Missing local mock JSON for " + integrationType + "/" + scenarioKey);
    }

    private MockIntegrationResult read(Path path) {
        log.debug("Loading mock integration response from local file path={}", path);
        try {
            return mapper.readValue(Files.readString(path), MockIntegrationResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid local mock JSON at " + path, e);
        }
    }

    private MockIntegrationResult readClasspath(String integrationType, String filename) {
        String resourcePath = "mock-data/" + integrationType + "/" + filename;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            log.debug("Loading mock integration response from classpath resource path={}", resourcePath);
            return mapper.readValue(in, MockIntegrationResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid classpath mock JSON at " + resourcePath, e);
        }
    }

    private Path resolveBasePath(String configuredPath) {
        Path configured = Path.of(configuredPath);
        if (Files.isDirectory(configured)) {
            return configured;
        }

        if (configured.isAbsolute()) {
            return configured;
        }

        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(configuredPath);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }

        return configured;
    }
}
