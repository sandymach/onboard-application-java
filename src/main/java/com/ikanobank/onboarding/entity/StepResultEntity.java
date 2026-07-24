package com.ikanobank.onboarding.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "step_result", uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "step_code"}))
@Getter
@Setter
public class StepResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "step_result_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    @Column(name = "application_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID applicationId;
    private String stepCode;
    @Lob
    private String answersJson;
    private String answersFingerprint;
    private Instant completedAt;
}
