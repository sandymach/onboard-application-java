package com.ikanobank.onboarding.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer")
@Getter
@Setter
public class CustomerEntity {
    @Id
    @Column(name = "customer_id", length = 10, nullable = false, columnDefinition = "char(10)")
    private String id;
    @Column(name = "application_id", columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID applicationId;
    private String customerType;
    private String country;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
