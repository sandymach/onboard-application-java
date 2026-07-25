package com.ikanobank.onboarding.orchestration;

import java.util.UUID;

import org.slf4j.Logger;

import io.temporal.workflow.Workflow;

public class IdvJourneyWorkflowImpl implements IdvJourneyWorkflow {
    private static final Logger log = Workflow.getLogger(IdvJourneyWorkflowImpl.class);

    @Override
    public void run(UUID applicationId, String country, String customerType) {
        log.info("IDV journey completed applicationId={} country={} customerType={}", applicationId, country, customerType);
    }
}
