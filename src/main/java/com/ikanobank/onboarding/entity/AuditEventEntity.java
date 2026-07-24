package com.ikanobank.onboarding.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "application_events")
@Getter
@Setter
public class AuditEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "application_event_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    @Column(name = "application_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID applicationId;
    private String eventType;
    private String resultCode;
    private String requestId;
    private Instant occurredAt;
}
