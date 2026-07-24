package com.ikanobank.onboarding.api;

import com.ikanobank.onboarding.domain.ApplicationStatus;

import jakarta.validation.constraints.NotNull;

public record ManualOverrideRequest(@NotNull ApplicationStatus status, String reason) {
}
