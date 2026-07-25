package com.ikanobank.onboarding.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.ikanobank.onboarding.entity.AuditEventEntity;
import com.ikanobank.onboarding.repository.AuditEventRepository;

@Service
public class AuditService {
    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo) {
        this.repo = repo;
    }

    public void record(UUID applicationId, String eventType, String resultCode) {
        AuditEventEntity event = new AuditEventEntity();
        event.setApplicationId(applicationId);
        event.setEventType(eventType);
        event.setResultCode(resultCode);
        event.setRequestId(MDC.get("requestId"));
        event.setOccurredAt(Instant.now());
        repo.save(event);
    }

    public List<AuditEventEntity> history(UUID applicationId) {
        return repo.findByApplicationIdOrderByOccurredAt(applicationId);
    }
}
