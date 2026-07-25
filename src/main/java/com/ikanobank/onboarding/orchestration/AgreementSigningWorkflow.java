package com.ikanobank.onboarding.orchestration;

import java.util.UUID;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface AgreementSigningWorkflow {
    @WorkflowMethod
    void run(UUID applicationId);
}
