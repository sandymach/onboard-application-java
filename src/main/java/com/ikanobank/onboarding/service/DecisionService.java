package com.ikanobank.onboarding.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ikanobank.onboarding.domain.ApplicationStatus;
import com.ikanobank.onboarding.domain.CheckOutcome;
import com.ikanobank.onboarding.entity.IntegrationResultEntity;

@Service
public class DecisionService {
    public ApplicationStatus decide(List<IntegrationResultEntity> results) {
        if (results.stream().anyMatch(result -> result.getOutcome() == CheckOutcome.FAIL)) {
            return ApplicationStatus.DECLINED;
        }
        if (results.stream().anyMatch(result -> result.getOutcome() == CheckOutcome.MANUAL_REVIEW
                || result.getOutcome() == CheckOutcome.UNAVAILABLE)) {
            return ApplicationStatus.MANUAL_REVIEW;
        }
        return ApplicationStatus.AGREEMENT_CREATED;
    }
}
