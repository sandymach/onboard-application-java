package com.ikanobank.onboarding.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "applicant")
@Getter
@Setter
public class ApplicantEntity {
    @Id
    @Column(name = "applicant_id", length = 10, nullable = false, columnDefinition = "char(10)")
    private String id;
    @Column(name = "application_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID applicationId;
    private String applicantType;
    private String country;
    private Instant createdAt;
    private Instant updatedAt;
}
