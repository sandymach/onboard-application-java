package com.ikanobank.onboarding.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ResumeTokenServiceTest {
    private final ResumeTokenService service = new ResumeTokenService();

    @Test
    void createsOpaqueTokensAndHashesThemDeterministically() {
        String token = service.createToken();

        assertThat(token).isNotBlank();
        assertThat(service.hash(token)).isEqualTo(service.hash(token));
        assertThat(service.hash(token)).isNotEqualTo(token);
    }

    @Test
    void rejectsBlankToken() {
        assertThatThrownBy(() -> service.hash(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resume token is required");
    }
}
