package com.ikanobank.onboarding.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ikanobank.onboarding.domain.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "decision")
@Getter
@Setter
public class DecisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "decision_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    @Column(name = "application_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID applicationId;
    @Enumerated(EnumType.STRING)
    private ApplicationStatus decisionStatus;
    private String reasonCode;
    private String decisionSource;
    private String decidedBy;
    private String requestId;
    private Instant decidedAt;
}
