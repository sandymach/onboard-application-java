package com.ikanobank.onboarding.api;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(UUID applicationId, String eventType, String details, Instant createdAt) {
}
