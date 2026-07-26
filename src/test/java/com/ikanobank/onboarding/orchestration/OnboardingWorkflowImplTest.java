package com.ikanobank.onboarding.orchestration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OnboardingWorkflowImplTest {
    @Test
    void exposesWorkflowStateForSignals() {
        OnboardingWorkflowImpl workflow = new OnboardingWorkflowImpl();
        workflow.stepCompleted("identity");
        workflow.checksRequested();
        workflow.agreementCreated();
        workflow.agreementSigned();
        workflow.accountSetupCompleted();
        workflow.terminalDecision("DECLINED");

        OnboardingWorkflowState state = workflow.state();

        assertThat(state.lastCompletedStepCode()).isEqualTo("identity");
        assertThat(state.checksRequested()).isTrue();
        assertThat(state.agreementCreated()).isTrue();
        assertThat(state.agreementSigned()).isTrue();
        assertThat(state.accountSetupCompleted()).isTrue();
        assertThat(state.terminalStatus()).isEqualTo("DECLINED");
        assertThat(state.completed()).isTrue();
    }
}
