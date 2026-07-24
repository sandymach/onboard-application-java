package com.ikanobank.onboarding.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ikanobank.onboarding.domain.CheckOutcome;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "integration_result")
@Getter
@Setter
public class IntegrationResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "integration_result_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    @Column(name = "application_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID applicationId;
    private String integrationType;
    @Enumerated(EnumType.STRING)
    private CheckOutcome outcome;
    private String reasonCode;
    private String message;
    private String answersFingerprint;
    private String requestId;
    private Instant checkedAt;
}
