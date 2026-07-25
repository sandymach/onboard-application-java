package com.ikanobank.onboarding.orchestration;

import java.util.UUID;

import org.slf4j.Logger;

import io.temporal.workflow.Workflow;

public class AgreementSigningWorkflowImpl implements AgreementSigningWorkflow {
    private static final Logger log = Workflow.getLogger(AgreementSigningWorkflowImpl.class);

    @Override
    public void run(UUID applicationId) {
        log.info("Agreement/signing reusable workflow completed applicationId={}", applicationId);
    }
}
