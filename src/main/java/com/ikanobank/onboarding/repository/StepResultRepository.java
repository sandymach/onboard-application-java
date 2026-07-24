package com.ikanobank.onboarding.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ikanobank.onboarding.entity.StepResultEntity;

public interface StepResultRepository extends JpaRepository<StepResultEntity, UUID> {
    Optional<StepResultEntity> findByApplicationIdAndStepCode(UUID applicationId, String stepCode);

    List<StepResultEntity> findByApplicationIdOrderByCompletedAt(UUID applicationId);
}
