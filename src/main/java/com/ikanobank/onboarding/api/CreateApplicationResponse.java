package com.ikanobank.onboarding.api;

public record CreateApplicationResponse(ApplicationResponse application, String resumeToken) {
}
