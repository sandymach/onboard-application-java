package com.ikanobank.onboarding.api;

import java.time.Instant;

import com.ikanobank.onboarding.domain.CheckOutcome;

public record IntegrationResultResponse(String integrationType, CheckOutcome outcome, String reasonCode, String message,
                                        Instant checkedAt) {
}
