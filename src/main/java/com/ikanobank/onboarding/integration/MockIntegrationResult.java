package com.ikanobank.onboarding.integration;

import java.util.Map;

import com.ikanobank.onboarding.domain.CheckOutcome;

public record MockIntegrationResult(CheckOutcome outcome, String reasonCode, String message,
                                    Map<String, String> attributes) {
}
