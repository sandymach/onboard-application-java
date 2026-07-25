package com.ikanobank.onboarding.orchestration;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;

@Component
@ConditionalOnProperty(prefix = "app.temporal", name = "enabled", havingValue = "true")
public class TemporalWorkflowGateway {
    private static final Logger log = LoggerFactory.getLogger(TemporalWorkflowGateway.class);

    private final WorkflowClient workflowClient;
    private final TemporalProperties properties;

    public TemporalWorkflowGateway(WorkflowClient workflowClient, TemporalProperties properties) {
        this.workflowClient = workflowClient;
        this.properties = properties;
    }

    public void start(UUID applicationId, String productCode, String country, String customerType) {
        String workflowId = workflowId(applicationId);
        OnboardingWorkflow workflow = workflowClient.newWorkflowStub(OnboardingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(properties.taskQueue())
                        .build());
        try {
            WorkflowClient.start(workflow::start, applicationId, productCode, country, customerType);
            log.info("Started Temporal workflow workflowId={}", workflowId);
        } catch (Exception ex) {
            log.warn("Temporal workflow start skipped workflowId={} reason={}", workflowId, ex.getClass().getSimpleName());
        }
    }

    public void stepCompleted(UUID applicationId, String stepCode) {
        stub(applicationId).stepCompleted(stepCode);
    }

    public void checksRequested(UUID applicationId, String country, String customerType) {
        stub(applicationId).checksRequested();
        startIdvWorkflow(applicationId, country, customerType);
    }

    public void agreementCreated(UUID applicationId) {
        stub(applicationId).agreementCreated();
        startAgreementSigningWorkflow(applicationId);
    }

    public void agreementSigned(UUID applicationId) {
        stub(applicationId).agreementSigned();
    }

    public void accountSetupCompleted(UUID applicationId) {
        stub(applicationId).accountSetupCompleted();
    }

    public void terminalDecision(UUID applicationId, String status) {
        stub(applicationId).terminalDecision(status);
    }

    private OnboardingWorkflow stub(UUID applicationId) {
        return workflowClient.newWorkflowStub(OnboardingWorkflow.class, workflowId(applicationId));
    }

    private String workflowId(UUID applicationId) {
        return "onboarding-" + applicationId;
    }

    private void startIdvWorkflow(UUID applicationId, String country, String customerType) {
        String workflowId = "idv-" + applicationId;
        IdvJourneyWorkflow workflow = workflowClient.newWorkflowStub(IdvJourneyWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(properties.taskQueue())
                        .build());
        try {
            WorkflowClient.start(workflow::run, applicationId, country, customerType);
            log.info("Started Temporal IDV workflow workflowId={}", workflowId);
        } catch (Exception ex) {
            log.warn("Temporal IDV workflow start skipped workflowId={} reason={}", workflowId, ex.getClass().getSimpleName());
        }
    }

    private void startAgreementSigningWorkflow(UUID applicationId) {
        String workflowId = "agreement-signing-" + applicationId;
        AgreementSigningWorkflow workflow = workflowClient.newWorkflowStub(AgreementSigningWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(properties.taskQueue())
                        .build());
        try {
            WorkflowClient.start(workflow::run, applicationId);
            log.info("Started Temporal agreement/signing workflow workflowId={}", workflowId);
        } catch (Exception ex) {
            log.warn("Temporal agreement/signing workflow start skipped workflowId={} reason={}", workflowId, ex.getClass().getSimpleName());
        }
    }
}
