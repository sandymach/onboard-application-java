package com.ikanobank.onboarding.api;

import java.time.*;
import java.util.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;

import com.ikanobank.onboarding.domain.*;
import com.ikanobank.onboarding.generated.api.*;
import com.ikanobank.onboarding.service.*;

import jakarta.validation.Valid;

@RestController
public class OnboardingController implements ApplicationsApi, FlowsApi, DecisioningApi, FulfilmentApi, AuditApi {
    private final OnboardingService service;
    private final AuditService audit;
    private final StepAnswerPolicy answerPolicy;

    public OnboardingController(OnboardingService service, AuditService audit, StepAnswerPolicy answerPolicy) {
        this.service = service;
        this.audit = audit;
        this.answerPolicy = answerPolicy;
    }

    @Override
    public ResponseEntity<com.ikanobank.onboarding.generated.model.CreateApplicationResponse> createApplication(
            @Valid com.ikanobank.onboarding.generated.model.CreateApplicationRequest request,
            String xTransactionId,
            String xProductCode,
            String xChannel) {
        CreateApplicationResponse response = service.create(new CreateApplicationRequest(
                country(request.getCountry()),
                customerType(request.getCustomerType()),
                request.getScenarioKey()));
        return ResponseEntity.status(HttpStatus.CREATED).body(createResponse(response));
    }

    @Override
    public ResponseEntity<com.ikanobank.onboarding.generated.model.ApplicationResponse> resumeApplication(
            @Valid com.ikanobank.onboarding.generated.model.ResumeApplicationRequest request) {
        return ResponseEntity.ok(application(service.resume(request.getResumeToken())));
    }

    @Override
    public ResponseEntity<com.ikanobank.onboarding.generated.model.ApplicationResponse> getApplication(UUID applicationId) {
        return ResponseEntity.ok(application(service.get(applicationId)));
    }

    @Override
    public ResponseEntity<com.ikanobank.onboarding.generated.model.ApplicationResponse> submitStep(
            UUID applicationId,
            String stepCode,
            @Valid com.ikanobank.onboarding.generated.model.SubmitStepRequest request,
            String xTransactionId,
            String xProductCode,
            String xChannel) {
        return ResponseEntity.ok(application(service.submitStep(applicationId, stepCode,
                new SubmitStepRequest(new LinkedHashMap<>(request.getAnswers())))));
    }

    @Override
    public ResponseEntity<List<com.ikanobank.onboarding.generated.model.FlowDefinition>> listFlows() {
        return ResponseEntity.ok(service.flows().stream().map(this::flow).toList());
    }

    @Override
    public ResponseEntity<com.ikanobank.onboarding.generated.model.FlowDefinition> getFlow(
            com.ikanobank.onboarding.generated.model.Country country,
            com.ikanobank.onboarding.generated.model.CustomerType type) {
        return ResponseEntity.ok(flow(service.flow(country(country), customerType(type))));
    }

    @Override
    public ResponseEntity<List<String>> getRequiredFields(
            com.ikanobank.onboarding.generated.model.Country country,
            com.ikanobank.onboarding.generated.model.CustomerType type,
            String stepCode) {
        return ResponseEntity.ok(new ArrayList<>(answerPolicy.requiredFields(country(country), customerType(type), stepCode)));
    }

    @Override
    public ResponseEntity<List<com.ikanobank.onboarding.generated.model.IntegrationResultResponse>> runChecks(UUID applicationId) {
        return ResponseEntity.ok(service.runChecks(applicationId).stream().map(this::integration).toList());
    }

    @Override
    public ResponseEntity<com.ikanobank.onboarding.generated.model.ApplicationResponse> submitApplication(UUID applicationId) {
        return ResponseEntity.ok(application(service.submit(applicationId)));
    }

    @Override
    public ResponseEntity<com.ikanobank.onboarding.generated.model.ApplicationResponse> manualOverride(
            UUID applicationId,
            @Valid com.ikanobank.onboarding.generated.model.ManualOverrideRequest request) {
        return ResponseEntity.ok(application(service.manualOverride(applicationId,
                ApplicationStatus.valueOf(request.getStatus().getValue()), request.getReason())));
    }

    @Override
    public ResponseEntity<com.ikanobank.onboarding.generated.model.IntegrationResultResponse> createAgreement(UUID applicationId) {
        return ResponseEntity.ok(integration(service.createAgreement(applicationId)));
    }

    @Override
    public ResponseEntity<com.ikanobank.onboarding.generated.model.IntegrationResultResponse> signLater(UUID applicationId) {
        return ResponseEntity.ok(integration(service.signLater(applicationId)));
    }

    @Override
    public ResponseEntity<com.ikanobank.onboarding.generated.model.IntegrationResultResponse> signAgreement(UUID applicationId) {
        return ResponseEntity.ok(integration(service.signAgreement(applicationId)));
    }

    @Override
    public ResponseEntity<com.ikanobank.onboarding.generated.model.IntegrationResultResponse> setupAccount(UUID applicationId) {
        return ResponseEntity.ok(integration(service.setupAccount(applicationId)));
    }

    @Override
    public ResponseEntity<List<com.ikanobank.onboarding.generated.model.AuditEventResponse>> getAuditEvents(UUID applicationId) {
        return ResponseEntity.ok(audit.history(applicationId).stream()
                .map(e -> new com.ikanobank.onboarding.generated.model.AuditEventResponse()
                        .applicationId(e.getApplicationId())
                        .eventType(e.getEventType())
                        .resultCode(e.getResultCode())
                        .occurredAt(offset(e.getOccurredAt())))
                .toList());
    }

    private com.ikanobank.onboarding.generated.model.CreateApplicationResponse createResponse(CreateApplicationResponse response) {
        return new com.ikanobank.onboarding.generated.model.CreateApplicationResponse()
                .application(application(response.application()))
                .resumeToken(response.resumeToken());
    }

    private com.ikanobank.onboarding.generated.model.ApplicationResponse application(ApplicationResponse response) {
        com.ikanobank.onboarding.generated.model.ApplicationResponse generated =
                new com.ikanobank.onboarding.generated.model.ApplicationResponse(
                        response.id(),
                        generatedCountry(response.country()),
                        generatedCustomerType(response.customerType()),
                        com.ikanobank.onboarding.generated.model.ApplicationStatus.fromValue(response.status().name()));
        generated.currentStepCode(response.currentStepCode())
                .scenarioKey(response.scenarioKey())
                .createdAt(offset(response.createdAt()))
                .updatedAt(offset(response.updatedAt()))
                .expiresAt(offset(response.expiresAt()))
                .submittedAt(offset(response.submittedAt()))
                .decisionReason(response.decisionReason());
        return generated;
    }

    private com.ikanobank.onboarding.generated.model.IntegrationResultResponse integration(IntegrationResultResponse response) {
        return new com.ikanobank.onboarding.generated.model.IntegrationResultResponse()
                .integrationType(response.integrationType())
                .outcome(com.ikanobank.onboarding.generated.model.CheckOutcome.fromValue(response.outcome().name()))
                .reasonCode(response.reasonCode())
                .message(response.message())
                .checkedAt(offset(response.checkedAt()));
    }

    private com.ikanobank.onboarding.generated.model.FlowDefinition flow(com.ikanobank.onboarding.flow.FlowDefinition response) {
        return new com.ikanobank.onboarding.generated.model.FlowDefinition()
                .country(generatedCountry(response.country()))
                .customerType(generatedCustomerType(response.customerType()))
                .steps(response.steps().stream()
                        .sorted(Comparator.comparingInt(com.ikanobank.onboarding.flow.FlowStepDefinition::order))
                        .map(step -> flowStep(response.country(), response.customerType(), step))
                        .toList());
    }

    private com.ikanobank.onboarding.generated.model.FlowStepDefinition flowStep(
            Country country,
            CustomerType customerType,
            com.ikanobank.onboarding.flow.FlowStepDefinition step) {
        return new com.ikanobank.onboarding.generated.model.FlowStepDefinition()
                .order(step.order())
                .code(step.code())
                .title(step.title())
                .fields(new ArrayList<>(answerPolicy.requiredFields(country, customerType, step.code())))
                .integrations(step.integrations());
    }

    private Country country(com.ikanobank.onboarding.generated.model.Country country) {
        return Country.valueOf(country.getValue());
    }

    private CustomerType customerType(com.ikanobank.onboarding.generated.model.CustomerType customerType) {
        return CustomerType.valueOf(customerType.getValue());
    }

    private com.ikanobank.onboarding.generated.model.Country generatedCountry(Country country) {
        return com.ikanobank.onboarding.generated.model.Country.fromValue(country.name());
    }

    private com.ikanobank.onboarding.generated.model.CustomerType generatedCustomerType(CustomerType customerType) {
        return com.ikanobank.onboarding.generated.model.CustomerType.fromValue(customerType.name());
    }

    private OffsetDateTime offset(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
