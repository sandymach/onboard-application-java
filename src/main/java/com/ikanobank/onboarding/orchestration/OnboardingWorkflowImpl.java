package com.ikanobank.onboarding.orchestration;

import java.util.UUID;

import io.temporal.workflow.Workflow;

public class OnboardingWorkflowImpl implements OnboardingWorkflow {
    private UUID applicationId;
    private String productCode;
    private String country;
    private String customerType;
    private String lastCompletedStepCode;
    private boolean checksRequested;
    private boolean agreementCreated;
    private boolean agreementSigned;
    private boolean accountSetupCompleted;
    private String terminalStatus;
    private boolean completed;

    @Override
    public void start(UUID applicationId, String productCode, String country, String customerType) {
        this.applicationId = applicationId;
        this.productCode = productCode;
        this.country = country;
        this.customerType = customerType;
        while (!completed) {
            Workflow.await(() -> accountSetupCompleted || completed);
            if (accountSetupCompleted) {
                completed = true;
            }
        }
    }

    @Override
    public void stepCompleted(String stepCode) {
        this.lastCompletedStepCode = stepCode;
    }

    @Override
    public void checksRequested() {
        this.checksRequested = true;
    }

    @Override
    public void agreementCreated() {
        this.agreementCreated = true;
    }

    @Override
    public void agreementSigned() {
        this.agreementSigned = true;
    }

    @Override
    public void accountSetupCompleted() {
        this.accountSetupCompleted = true;
    }

    @Override
    public void terminalDecision(String status) {
        this.terminalStatus = status;
        this.completed = true;
    }

    @Override
    public OnboardingWorkflowState state() {
        return new OnboardingWorkflowState(applicationId, productCode, country, customerType, lastCompletedStepCode,
                checksRequested, agreementCreated, agreementSigned, accountSetupCompleted, terminalStatus, completed);
    }
}
