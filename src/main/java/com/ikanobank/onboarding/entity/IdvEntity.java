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
@Table(name = "idv")
@Getter
@Setter
public class IdvEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "idv_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    @Column(name = "application_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID applicationId;
    @Column(name = "applicant_id", length = 10, columnDefinition = "char(10)")
    private String applicantId;
    @Enumerated(EnumType.STRING)
    private CheckOutcome outcome;
    private String provider;
    private String reasonCode;
    private String message;
    private String requestId;
    private Instant checkedAt;
}
