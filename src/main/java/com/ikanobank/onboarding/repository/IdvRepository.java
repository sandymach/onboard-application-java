package com.ikanobank.onboarding.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ikanobank.onboarding.entity.IdvEntity;

public interface IdvRepository extends JpaRepository<IdvEntity, UUID> {
}
