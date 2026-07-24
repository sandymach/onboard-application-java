package com.ikanobank.onboarding.api;

import jakarta.validation.constraints.NotBlank;

public record ResumeApplicationRequest(@NotBlank String resumeToken) {
}
