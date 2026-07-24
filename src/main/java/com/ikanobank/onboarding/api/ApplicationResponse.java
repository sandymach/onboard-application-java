package com.ikanobank.onboarding.api;

import java.time.Instant;
import java.util.UUID;

import com.ikanobank.onboarding.domain.*;

public record ApplicationResponse(UUID id, Country country, CustomerType customerType, String currentStepCode,
                                  ApplicationStatus status, String scenarioKey, Instant createdAt, Instant updatedAt,
                                  Instant expiresAt, Instant submittedAt, String decisionReason) {
}
