package com.ikanobank.onboarding.api;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record SubmitStepRequest(@NotNull Map<String, Object> answers) {
}
