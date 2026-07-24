package com.ikanobank.onboarding.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ikanobank.onboarding.entity.OnboardingApplicationEntity;

public interface OnboardingApplicationRepository extends JpaRepository<OnboardingApplicationEntity, UUID> {
    Optional<OnboardingApplicationEntity> findByResumeTokenHash(String resumeTokenHash);
}
