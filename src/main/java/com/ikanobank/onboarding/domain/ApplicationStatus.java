package com.ikanobank.onboarding.domain;

public enum ApplicationStatus {
    CREATED,
    INITIATED,
    IN_PROGRESS,
    KYC,
    IDV,
    READY_FOR_REVIEW,
    MANUAL_REVIEW,
    DECLINED,
    AGREEMENT_CREATED,
    SIGNING_PENDING,
    AGREEMENT_SIGNED,
    ACCOUNT_SETUP_COMPLETE,
    APPROVED,
    CANCELLED,
    EXPIRED,
    ABANDONED
}
