package com.ikanobank.onboarding.service;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikanobank.onboarding.api.*;
import com.ikanobank.onboarding.domain.*;
import com.ikanobank.onboarding.entity.*;
import com.ikanobank.onboarding.flow.*;
import com.ikanobank.onboarding.integration.*;
import com.ikanobank.onboarding.orchestration.TemporalWorkflowGateway;
import com.ikanobank.onboarding.repository.*;

@Service
public class OnboardingService {
    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private final OnboardingApplicationRepository apps;
    private final StepResultRepository steps;
    private final IntegrationResultRepository integrations;
    private final DecisionRepository decisions;
    private final IdvRepository idv;
    private final AgreementRepository agreements;
    private final SigningRepository signings;
    private final ApplicantRepository applicants;
    private final CustomerRepository customers;
    private final FlowRegistry flows;
    private final Map<String, MockIntegrationClient> clients;
    private final ObjectMapper mapper;
    private final AuditService audit;
    private final DecisionService decision;
    private final ResumeTokenService resumeTokens;
    private final StepAnswerPolicy answerPolicy;
    private final Optional<TemporalWorkflowGateway> temporal;

    public OnboardingService(OnboardingApplicationRepository apps, StepResultRepository steps,
                             IntegrationResultRepository integrations, DecisionRepository decisions,
                             IdvRepository idv, AgreementRepository agreements, SigningRepository signings,
                             ApplicantRepository applicants, CustomerRepository customers, FlowRegistry flows,
                             List<MockIntegrationClient> clients, ObjectMapper mapper, AuditService audit,
                             DecisionService decision, ResumeTokenService resumeTokens,
                             StepAnswerPolicy answerPolicy, ObjectProvider<TemporalWorkflowGateway> temporal) {
        this.apps = apps;
        this.steps = steps;
        this.integrations = integrations;
        this.decisions = decisions;
        this.idv = idv;
        this.agreements = agreements;
        this.signings = signings;
        this.applicants = applicants;
        this.customers = customers;
        this.flows = flows;
        this.clients = clients.stream().collect(Collectors.toMap(MockIntegrationClient::type, Function.identity()));
        this.mapper = mapper;
        this.audit = audit;
        this.decision = decision;
        this.resumeTokens = resumeTokens;
        this.answerPolicy = answerPolicy;
        this.temporal = Optional.ofNullable(temporal.getIfAvailable());
    }

    @Transactional
    public CreateApplicationResponse create(CreateApplicationRequest request) {
        FlowDefinition flow = flows.get(request.country(), request.customerType());
        String token = resumeTokens.createToken();
        Instant now = Instant.now();

        OnboardingApplicationEntity entity = new OnboardingApplicationEntity();
        entity.setId(UUID.randomUUID());
        entity.setCountry(request.country());
        entity.setCustomerType(request.customerType());
        entity.setCurrentStepCode(firstStep(flow).code());
        entity.setStatus(ApplicationStatus.INITIATED);
        entity.setScenarioKey(normalizedScenarioKey(request.scenarioKey()));
        entity.setResumeTokenHash(resumeTokens.hash(token));
        entity.setResumeTokenExpiresAt(now.plus(7, ChronoUnit.DAYS));
        entity.setExpiresAt(now.plus(30, ChronoUnit.DAYS));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        apps.save(entity);
        createApplicantAndCustomer(entity, now);

        putApplicationMdc(entity);
        audit.record(entity.getId(), "APPLICATION_CREATED", null);
        this.temporal.ifPresent(gateway -> gateway.start(entity.getId(), "ONBOARDING",
                entity.getCountry().name(), entity.getCustomerType().name()));
        log.info("Application initiated country={} customerType={} firstStep={}", entity.getCountry(), entity.getCustomerType(),
                entity.getCurrentStepCode());
        return new CreateApplicationResponse(map(entity), token);
    }

    @Transactional
    public ApplicationResponse resume(String token) {
        OnboardingApplicationEntity entity = apps.findByResumeTokenHash(resumeTokens.hash(token))
                .orElseThrow(() -> new NoSuchElementException("Application not found for resume token"));
        ensureNotExpired(entity);
        audit.record(entity.getId(), "APPLICATION_RESUMED", entity.getCurrentStepCode());
        return map(entity);
    }

    @Transactional
    public ApplicationResponse submitStep(UUID id, String stepCode, SubmitStepRequest request) {
        OnboardingApplicationEntity entity = getMutableEntity(id);
        MDC.put("applicationId", id.toString());
        putApplicationMdc(entity);
        FlowDefinition flow = flows.get(entity.getCountry(), entity.getCustomerType());
        List<FlowStepDefinition> ordered = orderedSteps(flow);
        int expected = expectedStepIndex(entity, ordered);
        FlowStepDefinition expectedStep = ordered.get(expected);
        if (!expectedStep.code().equals(stepCode)) {
            throw new IllegalStateException("Expected step " + expectedStep.code() + " but received " + stepCode);
        }

        answerPolicy.validate(entity.getCountry(), entity.getCustomerType(), stepCode, request.answers());
        String fingerprint = answerPolicy.fingerprint(request.answers());
        StepResultEntity result = steps.findByApplicationIdAndStepCode(id, stepCode).orElseGet(StepResultEntity::new);
        if (result.getId() == null) {
            result.setApplicationId(id);
            result.setStepCode(stepCode);
        }
        try {
            result.setAnswersJson(mapper.writeValueAsString(request.answers()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid step answers", ex);
        }
        result.setAnswersFingerprint(fingerprint);
        result.setCompletedAt(Instant.now());
        steps.save(result);

        integrations.deleteAll(integrations.findByApplicationId(id));
        if (expected + 1 < ordered.size()) {
            entity.setCurrentStepCode(ordered.get(expected + 1).code());
            if ("identity".equals(stepCode) || "representative".equals(stepCode) || "owners".equals(stepCode)) {
                entity.setStatus(ApplicationStatus.IDV);
            } else if ("compliance".equals(stepCode) || "company".equals(stepCode)) {
                entity.setStatus(ApplicationStatus.KYC);
            } else {
                entity.setStatus(ApplicationStatus.IN_PROGRESS);
            }
        } else {
            entity.setStatus(ApplicationStatus.READY_FOR_REVIEW);
        }
        entity.setUpdatedAt(Instant.now());
        apps.save(entity);
        if (entity.getStatus() == ApplicationStatus.IDV) {
            saveApplicantSnapshot(entity);
        }
        audit.record(id, "STEP_COMPLETED", stepCode);
        temporal.ifPresent(gateway -> gateway.stepCompleted(id, stepCode));
        log.info("Step completed stepCode={} nextStep={} status={}", stepCode, entity.getCurrentStepCode(), entity.getStatus());
        return map(entity);
    }

    @Transactional
    public List<IntegrationResultResponse> runChecks(UUID id) {
        OnboardingApplicationEntity entity = getMutableEntity(id);
        putApplicationMdc(entity);
        if (entity.getStatus() != ApplicationStatus.READY_FOR_REVIEW) {
            throw new IllegalStateException("Complete all steps before running external checks");
        }
        MDC.put("applicationId", id.toString());
        FlowDefinition flow = flows.get(entity.getCountry(), entity.getCustomerType());
        Set<String> requiredTypes = requiredIntegrationTypes(flow);
        integrations.deleteAll(integrations.findByApplicationId(id));
        temporal.ifPresent(gateway -> gateway.checksRequested(id, entity.getCountry().name(), entity.getCustomerType().name()));
        List<IntegrationResultEntity> saved = new ArrayList<>();
        String fingerprint = applicationFingerprint(id);
        log.info("Running integration checks requiredTypes={}", requiredTypes);

        for (String type : requiredTypes) {
            MockIntegrationClient client = clients.get(type);
            if (client == null) {
                log.warn("No mock client configured integrationType={}", type);
                continue;
            }
            saved.add(runCheck(id, entity.getScenarioKey(), type, client, fingerprint));
        }
        return saved.stream().map(this::map).toList();
    }

    @Transactional
    public ApplicationResponse submit(UUID id) {
        OnboardingApplicationEntity entity = getMutableEntity(id);
        putApplicationMdc(entity);
        if (entity.getStatus() != ApplicationStatus.READY_FOR_REVIEW) {
            throw new IllegalStateException("Application must be ready for review before final submission");
        }
        MDC.put("applicationId", id.toString());
        FlowDefinition flow = flows.get(entity.getCountry(), entity.getCustomerType());
        List<IntegrationResultEntity> results = integrations.findByApplicationId(id);
        Set<String> completedTypes = results.stream().map(IntegrationResultEntity::getIntegrationType).collect(Collectors.toSet());
        Set<String> requiredTypes = requiredIntegrationTypes(flow);
        if (!completedTypes.containsAll(requiredTypes)) {
            throw new IllegalStateException("Run all external checks before submitting");
        }
        ApplicationStatus decided = decision.decide(results);
        entity.setStatus(decided);
        entity.setDecisionReason(decisionReason(results, decided));
        entity.setSubmittedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        apps.save(entity);
        saveDecision(entity, "SYSTEM", null);
        audit.record(id, "FINAL_DECISION", entity.getStatus().name());
        if (entity.getStatus() == ApplicationStatus.DECLINED) {
            temporal.ifPresent(gateway -> gateway.terminalDecision(id, entity.getStatus().name()));
        }
        log.info("Decision completed status={} checkCount={} reason={}", entity.getStatus(), results.size(), entity.getDecisionReason());
        return map(entity);
    }

    @Transactional
    public IntegrationResultResponse createAgreement(UUID id) {
        OnboardingApplicationEntity entity = getEntity(id);
        putApplicationMdc(entity);
        ensureNotExpired(entity);
        if (entity.getStatus() != ApplicationStatus.AGREEMENT_CREATED
                && entity.getStatus() != ApplicationStatus.SIGNING_PENDING) {
            throw new IllegalStateException("Agreement can only be created after decision approval");
        }
        IntegrationResultEntity result = runLifecycleMock(id, entity, "agreement", "AGREEMENT_CREATED");
        saveAgreement(id, result);
        entity.setStatus(ApplicationStatus.AGREEMENT_CREATED);
        entity.setUpdatedAt(Instant.now());
        apps.save(entity);
        audit.record(id, "AGREEMENT_CREATED", result.getReasonCode());
        temporal.ifPresent(gateway -> gateway.agreementCreated(id));
        log.info("Agreement created outcome={} reason={}", result.getOutcome(), result.getReasonCode());
        return map(result);
    }

    @Transactional
    public IntegrationResultResponse signLater(UUID id) {
        OnboardingApplicationEntity entity = getEntity(id);
        putApplicationMdc(entity);
        ensureNotExpired(entity);
        if (entity.getStatus() != ApplicationStatus.AGREEMENT_CREATED
                && entity.getStatus() != ApplicationStatus.SIGNING_PENDING) {
            throw new IllegalStateException("Sign-later can only be selected after agreement creation");
        }
        IntegrationResultEntity result = runLifecycleMock(id, entity, "signing", "SIGN_LATER_SELECTED");
        saveSigning(id, result, "SIGN_LATER");
        entity.setStatus(ApplicationStatus.SIGNING_PENDING);
        entity.setUpdatedAt(Instant.now());
        apps.save(entity);
        audit.record(id, "SIGN_LATER_SELECTED", result.getReasonCode());
        return map(result);
    }

    @Transactional
    public IntegrationResultResponse signAgreement(UUID id) {
        OnboardingApplicationEntity entity = getEntity(id);
        putApplicationMdc(entity);
        ensureNotExpired(entity);
        if (entity.getStatus() != ApplicationStatus.AGREEMENT_CREATED
                && entity.getStatus() != ApplicationStatus.SIGNING_PENDING) {
            throw new IllegalStateException("Agreement can only be signed after agreement creation");
        }
        IntegrationResultEntity result = runLifecycleMock(id, entity, "signing", "AGREEMENT_SIGNED");
        saveSigning(id, result, "SIGN_NOW");
        entity.setStatus(ApplicationStatus.AGREEMENT_SIGNED);
        entity.setUpdatedAt(Instant.now());
        apps.save(entity);
        audit.record(id, "AGREEMENT_SIGNED", result.getReasonCode());
        temporal.ifPresent(gateway -> gateway.agreementSigned(id));
        log.info("Agreement signed outcome={} reason={}", result.getOutcome(), result.getReasonCode());
        return map(result);
    }

    @Transactional
    public IntegrationResultResponse setupAccount(UUID id) {
        OnboardingApplicationEntity entity = getEntity(id);
        putApplicationMdc(entity);
        ensureNotExpired(entity);
        if (entity.getStatus() != ApplicationStatus.AGREEMENT_SIGNED
                && entity.getStatus() != ApplicationStatus.ACCOUNT_SETUP_COMPLETE) {
            throw new IllegalStateException("Account setup can only run after agreement signing");
        }
        IntegrationResultEntity result = runLifecycleMock(id, entity, "account-setup", "ACCOUNT_SETUP_COMPLETED");
        entity.setStatus(ApplicationStatus.APPROVED);
        entity.setUpdatedAt(Instant.now());
        apps.save(entity);
        updateCustomerStatus(id, "EXISTING_CUSTOMER");
        audit.record(id, "ACCOUNT_SETUP_COMPLETED", result.getReasonCode());
        temporal.ifPresent(gateway -> gateway.accountSetupCompleted(id));
        log.info("Account setup completed outcome={} finalStatus={}", result.getOutcome(), entity.getStatus());
        return map(result);
    }

    @Transactional
    public ApplicationResponse manualOverride(UUID id, ApplicationStatus status, String reason) {
        OnboardingApplicationEntity entity = getEntity(id);
        putApplicationMdc(entity);
        if (entity.getStatus() != ApplicationStatus.MANUAL_REVIEW) {
            throw new IllegalStateException("Manual override is only allowed for manually reviewed applications");
        }
        if (status != ApplicationStatus.AGREEMENT_CREATED && status != ApplicationStatus.DECLINED && status != ApplicationStatus.CANCELLED) {
            throw new IllegalArgumentException("Manual override status must be AGREEMENT_CREATED, DECLINED or CANCELLED");
        }
        entity.setStatus(status);
        entity.setDecisionReason(reason == null || reason.isBlank() ? "MANUAL_OVERRIDE" : reason);
        entity.setUpdatedAt(Instant.now());
        apps.save(entity);
        saveDecision(entity, "MANUAL_OVERRIDE", MDC.get("requestId"));
        audit.record(id, "MANUAL_OVERRIDE", status.name());
        if (status == ApplicationStatus.DECLINED || status == ApplicationStatus.CANCELLED) {
            temporal.ifPresent(gateway -> gateway.terminalDecision(id, status.name()));
        }
        log.info("Manual override applied status={} reason={}", status, entity.getDecisionReason());
        return map(entity);
    }

    public ApplicationResponse get(UUID id) {
        return map(getEntity(id));
    }

    public FlowDefinition flow(Country country, CustomerType customerType) {
        return flows.get(country, customerType);
    }

    public Collection<FlowDefinition> flows() {
        return flows.all();
    }

    private IntegrationResultEntity runCheck(UUID id, String scenarioKey, String type, MockIntegrationClient client,
                                             String fingerprint) {
        try {
            MockIntegrationResult result = client.execute(scenarioKey);
            IntegrationResultEntity entity = new IntegrationResultEntity();
            entity.setApplicationId(id);
            entity.setIntegrationType(type);
            entity.setOutcome(result.outcome());
            entity.setReasonCode(result.reasonCode());
            entity.setMessage(result.message());
            entity.setAnswersFingerprint(fingerprint);
            entity.setRequestId(MDC.get("requestId"));
            entity.setCheckedAt(Instant.now());
            audit.record(id, "INTEGRATION_CHECK_COMPLETED", type + ":" + result.outcome());
            log.info("Integration completed type={} outcome={} reasonCode={}", type, result.outcome(), result.reasonCode());
            IntegrationResultEntity saved = integrations.save(entity);
            saveDomainCheck(id, type, saved);
            return saved;
        } catch (Exception ex) {
            log.error("Integration unavailable type={} error={}", type, ex.getClass().getSimpleName());
            IntegrationResultEntity entity = new IntegrationResultEntity();
            entity.setApplicationId(id);
            entity.setIntegrationType(type);
            entity.setOutcome(CheckOutcome.UNAVAILABLE);
            entity.setReasonCode("MOCK_SERVICE_UNAVAILABLE");
            entity.setMessage("Integration is temporarily unavailable");
            entity.setAnswersFingerprint(fingerprint);
            entity.setRequestId(MDC.get("requestId"));
            entity.setCheckedAt(Instant.now());
            audit.record(id, "INTEGRATION_CHECK_FAILED", type);
            IntegrationResultEntity saved = integrations.save(entity);
            saveDomainCheck(id, type, saved);
            return saved;
        }
    }

    private void createApplicantAndCustomer(OnboardingApplicationEntity entity, Instant now) {
        ApplicantEntity applicant = new ApplicantEntity();
        applicant.setId(readableNumericId("1", entity.getId()));
        applicant.setApplicationId(entity.getId());
        applicant.setApplicantType(entity.getCustomerType().name());
        applicant.setCountry(entity.getCountry().name());
        applicant.setCreatedAt(now);
        applicant.setUpdatedAt(now);
        applicants.save(applicant);

        CustomerEntity customer = new CustomerEntity();
        customer.setId(readableNumericId("2", entity.getId()));
        customer.setApplicationId(entity.getId());
        customer.setCustomerType(entity.getCustomerType().name());
        customer.setCountry(entity.getCountry().name());
        customer.setStatus("NEW_CUSTOMER");
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        customers.save(customer);
    }

    private void updateCustomerStatus(UUID applicationId, String status) {
        customers.findFirstByApplicationId(applicationId).ifPresent(customer -> {
            customer.setStatus(status);
            customer.setUpdatedAt(Instant.now());
            customers.save(customer);
        });
    }

    private void saveApplicantSnapshot(OnboardingApplicationEntity entity) {
        ApplicantEntity applicant = new ApplicantEntity();
        applicant.setId(readableNumericId("3", entity.getId()));
        applicant.setApplicationId(entity.getId());
        applicant.setApplicantType(entity.getCustomerType().name());
        applicant.setCountry(entity.getCountry().name());
        applicant.setCreatedAt(Instant.now());
        applicant.setUpdatedAt(Instant.now());
        applicants.save(applicant);
    }

    private String readableNumericId(String namespacePrefix, UUID applicationId) {
        long hash = Math.abs(ByteBuffer.wrap(applicationId.toString().getBytes(StandardCharsets.UTF_8)).getLong());
        long suffix = hash % 1_000_000_000L;
        return namespacePrefix + String.format("%09d", suffix);
    }

    private void saveDomainCheck(UUID applicationId, String type, IntegrationResultEntity result) {
        if ("identity".equals(type) || "pep".equals(type) || "sanctions".equals(type)) {
            IdvEntity entity = new IdvEntity();
            entity.setApplicationId(applicationId);
            entity.setProvider(type);
            entity.setOutcome(result.getOutcome());
            entity.setReasonCode(result.getReasonCode());
            entity.setMessage(result.getMessage());
            entity.setRequestId(result.getRequestId());
            entity.setCheckedAt(result.getCheckedAt());
            idv.save(entity);
        }
    }

    private void saveDecision(OnboardingApplicationEntity application, String source, String decidedBy) {
        DecisionEntity entity = new DecisionEntity();
        entity.setApplicationId(application.getId());
        entity.setDecisionStatus(application.getStatus());
        entity.setReasonCode(application.getDecisionReason());
        entity.setDecisionSource(source);
        entity.setDecidedBy(decidedBy);
        entity.setRequestId(MDC.get("requestId"));
        entity.setDecidedAt(Instant.now());
        decisions.save(entity);
    }

    private void saveAgreement(UUID applicationId, IntegrationResultEntity result) {
        AgreementEntity entity = new AgreementEntity();
        entity.setApplicationId(applicationId);
        entity.setAgreementReference("agreement-" + applicationId);
        entity.setOutcome(result.getOutcome());
        entity.setReasonCode(result.getReasonCode());
        entity.setMessage(result.getMessage());
        entity.setCreatedAt(Instant.now());
        agreements.save(entity);
    }

    private void saveSigning(UUID applicationId, IntegrationResultEntity result, String mode) {
        SigningEntity entity = new SigningEntity();
        entity.setApplicationId(applicationId);
        entity.setSigningReference("signing-" + applicationId);
        entity.setSigningMode(mode);
        entity.setOutcome(result.getOutcome());
        entity.setReasonCode(result.getReasonCode());
        entity.setMessage(result.getMessage());
        entity.setSignedAt(Instant.now());
        signings.save(entity);
    }

    private IntegrationResultEntity runLifecycleMock(UUID id, OnboardingApplicationEntity entity, String type,
                                                     String auditEvent) {
        MockIntegrationClient client = clients.get(type);
        if (client == null) {
            throw new IllegalStateException("No mock client configured for " + type);
        }
        IntegrationResultEntity result = runCheck(id, entity.getScenarioKey(), type, client, applicationFingerprint(id));
        if (result.getOutcome() == CheckOutcome.FAIL || result.getOutcome() == CheckOutcome.UNAVAILABLE) {
            throw new IllegalStateException(auditEvent + " failed: " + result.getReasonCode());
        }
        return result;
    }

    private OnboardingApplicationEntity getMutableEntity(UUID id) {
        OnboardingApplicationEntity entity = getEntity(id);
        ensureNotExpired(entity);
        if (EnumSet.of(ApplicationStatus.MANUAL_REVIEW, ApplicationStatus.DECLINED,
                ApplicationStatus.AGREEMENT_CREATED, ApplicationStatus.SIGNING_PENDING, ApplicationStatus.AGREEMENT_SIGNED,
                ApplicationStatus.ACCOUNT_SETUP_COMPLETE, ApplicationStatus.APPROVED, ApplicationStatus.CANCELLED,
                ApplicationStatus.ABANDONED).contains(entity.getStatus())) {
            throw new IllegalStateException("Application is no longer mutable: " + entity.getStatus());
        }
        return entity;
    }

    private void ensureNotExpired(OnboardingApplicationEntity entity) {
        Instant now = Instant.now();
        if ((entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(now))
                || (entity.getResumeTokenExpiresAt() != null && entity.getResumeTokenExpiresAt().isBefore(now))) {
            entity.setStatus(ApplicationStatus.EXPIRED);
            entity.setUpdatedAt(now);
            apps.save(entity);
            audit.record(entity.getId(), "APPLICATION_EXPIRED", null);
            throw new IllegalStateException("Application has expired");
        }
    }

    private OnboardingApplicationEntity getEntity(UUID id) {
        return apps.findById(id).orElseThrow(() -> new NoSuchElementException("Application not found: " + id));
    }

    private FlowStepDefinition firstStep(FlowDefinition flow) {
        return orderedSteps(flow).get(0);
    }

    private List<FlowStepDefinition> orderedSteps(FlowDefinition flow) {
        return flow.steps().stream().sorted(Comparator.comparingInt(FlowStepDefinition::order)).toList();
    }

    private int expectedStepIndex(OnboardingApplicationEntity entity, List<FlowStepDefinition> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).code().equals(entity.getCurrentStepCode())) {
                return i;
            }
        }
        throw new IllegalStateException("Current step is not part of the selected flow: " + entity.getCurrentStepCode());
    }

    private Set<String> requiredIntegrationTypes(FlowDefinition flow) {
        return flow.steps().stream().flatMap(step -> step.integrations().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String applicationFingerprint(UUID id) {
        String combined = steps.findByApplicationIdOrderByCompletedAt(id).stream()
                .map(StepResultEntity::getAnswersFingerprint)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(":"));
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to fingerprint application answers", ex);
        }
    }

    private String decisionReason(List<IntegrationResultEntity> results, ApplicationStatus status) {
        return results.stream()
                .filter(result -> result.getOutcome() == CheckOutcome.FAIL
                        || result.getOutcome() == CheckOutcome.MANUAL_REVIEW
                        || result.getOutcome() == CheckOutcome.UNAVAILABLE)
                .map(result -> result.getIntegrationType() + ":" + result.getReasonCode())
                .findFirst()
                .orElse(status.name());
    }

    private String normalizedScenarioKey(String scenarioKey) {
        return scenarioKey == null || scenarioKey.isBlank() ? "default" : scenarioKey;
    }

    private ApplicationResponse map(OnboardingApplicationEntity entity) {
        return new ApplicationResponse(entity.getId(), entity.getCountry(), entity.getCustomerType(),
                entity.getCurrentStepCode(), entity.getStatus(), entity.getScenarioKey(), entity.getCreatedAt(),
                entity.getUpdatedAt(), entity.getExpiresAt(), entity.getSubmittedAt(), entity.getDecisionReason());
    }

    private IntegrationResultResponse map(IntegrationResultEntity entity) {
        return new IntegrationResultResponse(entity.getIntegrationType(), entity.getOutcome(), entity.getReasonCode(),
                entity.getMessage(), entity.getCheckedAt());
    }

    private void putApplicationMdc(OnboardingApplicationEntity entity) {
        MDC.put("applicationId", entity.getId().toString());
        MDC.put("country", entity.getCountry().name());
        MDC.put("customerType", entity.getCustomerType().name());
    }
}
