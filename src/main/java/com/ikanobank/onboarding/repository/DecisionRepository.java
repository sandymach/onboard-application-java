package com.ikanobank.onboarding.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ikanobank.onboarding.entity.DecisionEntity;

public interface DecisionRepository extends JpaRepository<DecisionEntity, UUID> {
}
