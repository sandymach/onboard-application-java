package com.ikanobank.onboarding.service;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikanobank.onboarding.domain.*;

class StepAnswerPolicyTest {
    private final StepAnswerPolicy policy = new StepAnswerPolicy(new ObjectMapper());

    @Test
    void validatesCountrySpecificPrivateIdentityFields() {
        policy.validate(Country.SWEDEN, CustomerType.PRIVATE_INDIVIDUAL, "identity",
                Map.of("personalNumber", "19940608-1111"));
        policy.validate(Country.SPAIN, CustomerType.PRIVATE_INDIVIDUAL, "identity", Map.of("dniNie", "X1234567L"));
        policy.validate(Country.POLAND, CustomerType.PRIVATE_INDIVIDUAL, "identity", Map.of("pesel", "44051401458"));
    }

    @Test
    void rejectsMissingRequiredField() {
        assertThatThrownBy(() -> policy.validate(Country.SWEDEN, CustomerType.PRIVATE_INDIVIDUAL, "identity", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Step answers are required");
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> policy.validate(Country.SWEDEN, CustomerType.PRIVATE_INDIVIDUAL, "contact",
                Map.of("email", "bad-email", "phone", "+46701234567", "address", "Demo Street 12")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email");
    }

    @Test
    void rejectsUnderageApplicantWhereDateOfBirthIsAvailable() {
        assertThatThrownBy(() -> policy.validate(Country.POLAND, CustomerType.PRIVATE_INDIVIDUAL, "identity",
                Map.of("pesel", "22210112345")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 18");
    }

    @Test
    void rejectsUnsustainableDebtToIncomeRatio() {
        assertThatThrownBy(() -> policy.validate(Country.SWEDEN, CustomerType.PRIVATE_INDIVIDUAL, "financial",
                Map.of("employmentStatus", "PERMANENT", "monthlyIncome", "20000", "monthlyDebt", "15000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monthlyDebt is too high");
    }

    @Test
    void rejectsBusinessWithoutAuthorityConfirmation() {
        assertThatThrownBy(() -> policy.validate(Country.SWEDEN, CustomerType.BUSINESS, "representative",
                Map.of("representativeName", "Alex Demo", "representativeIdentifier", "19800101-1234",
                        "authorityConfirmed", "false")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("authorityConfirmed must be accepted");
    }

    @Test
    void fingerprintIsStableForEquivalentAnswerMaps() {
        String first = policy.fingerprint(Map.of("b", 2, "a", 1));
        String second = policy.fingerprint(Map.of("a", 1, "b", 2));

        assertThat(first).isEqualTo(second);
    }
}
