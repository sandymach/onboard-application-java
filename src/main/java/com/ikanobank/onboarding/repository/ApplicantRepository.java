package com.ikanobank.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ikanobank.onboarding.entity.ApplicantEntity;

public interface ApplicantRepository extends JpaRepository<ApplicantEntity, String> {
}
