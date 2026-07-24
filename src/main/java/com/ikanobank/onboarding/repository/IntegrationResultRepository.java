package com.ikanobank.onboarding.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ikanobank.onboarding.entity.IntegrationResultEntity;

public interface IntegrationResultRepository extends JpaRepository<IntegrationResultEntity, UUID> {
    List<IntegrationResultEntity> findByApplicationId(UUID applicationId);
}
