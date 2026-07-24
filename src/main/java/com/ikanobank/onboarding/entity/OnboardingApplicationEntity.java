package com.ikanobank.onboarding.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ikanobank.onboarding.domain.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "application")
@Getter
@Setter
public class OnboardingApplicationEntity {
    @Id
    @Column(name = "application_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    @Enumerated(EnumType.STRING)
    private Country country;
    @Enumerated(EnumType.STRING)
    private CustomerType customerType;
    private String currentStepCode;
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;
    private String scenarioKey;
    private String resumeTokenHash;
    private Instant resumeTokenExpiresAt;
    private Instant expiresAt;
    private String decisionReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant submittedAt;
}
