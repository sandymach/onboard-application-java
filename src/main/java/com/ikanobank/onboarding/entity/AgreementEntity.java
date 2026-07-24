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
@Table(name = "agreement")
@Getter
@Setter
public class AgreementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "agreement_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    @Column(name = "application_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID applicationId;
    private String agreementReference;
    @Enumerated(EnumType.STRING)
    private CheckOutcome outcome;
    private String reasonCode;
    private String message;
    private Instant createdAt;
}
