package com.ikanobank.onboarding.orchestration;

import java.util.UUID;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface IdvJourneyWorkflow {
    @WorkflowMethod
    void run(UUID applicationId, String country, String customerType);
}
