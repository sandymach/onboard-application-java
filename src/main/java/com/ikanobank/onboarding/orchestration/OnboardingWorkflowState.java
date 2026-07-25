package com.ikanobank.onboarding.orchestration;

import java.util.UUID;

public record OnboardingWorkflowState(UUID applicationId, String productCode, String country, String customerType,
                                      String lastCompletedStepCode, boolean checksRequested,
                                      boolean agreementCreated, boolean agreementSigned,
                                      boolean accountSetupCompleted, String terminalStatus, boolean completed) {
}
