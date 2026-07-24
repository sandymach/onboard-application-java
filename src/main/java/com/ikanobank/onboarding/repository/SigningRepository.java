package com.ikanobank.onboarding.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ikanobank.onboarding.entity.SigningEntity;

public interface SigningRepository extends JpaRepository<SigningEntity, UUID> {
}
