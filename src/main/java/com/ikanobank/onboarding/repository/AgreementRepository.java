package com.ikanobank.onboarding.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ikanobank.onboarding.entity.AgreementEntity;

public interface AgreementRepository extends JpaRepository<AgreementEntity, UUID> {
}
