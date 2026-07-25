package com.ikanobank.onboarding.orchestration;

import java.util.UUID;

import io.temporal.workflow.*;

@WorkflowInterface
public interface OnboardingWorkflow {
    @WorkflowMethod
    void start(UUID applicationId, String productCode, String country, String customerType);

    @SignalMethod
    void stepCompleted(String stepCode);

    @SignalMethod
    void checksRequested();

    @SignalMethod
    void agreementCreated();

    @SignalMethod
    void agreementSigned();

    @SignalMethod
    void accountSetupCompleted();

    @SignalMethod
    void terminalDecision(String status);

    @QueryMethod
    OnboardingWorkflowState state();
}
