package com.ikanobank.onboarding.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikanobank.onboarding.domain.*;
import com.ikanobank.onboarding.flow.FlowDefinition;
import com.ikanobank.onboarding.integration.*;
import com.ikanobank.onboarding.repository.CustomerRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnboardingControllerIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private LocalScenarioRepository scenarios;

    @Autowired
    private CustomerRepository customers;

    @ParameterizedTest
    @MethodSource("flows")
    void completesAllBackendFlows(Country country, CustomerType customerType) throws Exception {
        when(scenarios.load(anyString(), anyString()))
                .thenReturn(new MockIntegrationResult(CheckOutcome.PASS, "PASS", "Mock passed", Map.of()));

        JsonNode created = postJson("/api/v1/applications",
                Map.of("country", country.name(), "customerType", customerType.name(), "scenarioKey", "default"));
        String id = created.at("/application/id").asText();
        String resumeToken = created.at("/resumeToken").asText();

        mvc.perform(post("/api/v1/applications/resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("resumeToken", resumeToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        JsonNode flowJson = mvc.perform(get("/api/v1/flows/{country}/{type}", country, customerType))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .describeConstable()
                .map(this::readJson)
                .orElseThrow();

        for (JsonNode step : flowJson.get("steps")) {
            String stepCode = step.get("code").asText();
            mvc.perform(put("/api/v1/applications/{id}/steps/{stepCode}", id, stepCode)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("answers", answers(country, customerType, stepCode)))))
                    .andExpect(status().isOk());
        }

        mvc.perform(post("/api/v1/applications/{id}/checks", id))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/applications/{id}/submit", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGREEMENT_CREATED"));
        mvc.perform(post("/api/v1/applications/{id}/agreement", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrationType").value("agreement"))
                .andExpect(jsonPath("$.outcome").value("PASS"));
        mvc.perform(post("/api/v1/applications/{id}/agreement/sign", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrationType").value("signing"))
                .andExpect(jsonPath("$.outcome").value("PASS"));
        mvc.perform(post("/api/v1/applications/{id}/account-setup", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrationType").value("account-setup"))
                .andExpect(jsonPath("$.outcome").value("PASS"));
        mvc.perform(get("/api/v1/applications/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        assertThat(customers.findFirstByApplicationId(UUID.fromString(id)))
                .isPresent()
                .get()
                .extracting("status")
                .isEqualTo("EXISTING_CUSTOMER");
        mvc.perform(get("/api/v1/applications/{id}/audit-events", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @org.junit.jupiter.api.Test
    void returnsExpectedErrorResponsesForValidationNotFoundAndStateConflict() throws Exception {
        when(scenarios.load(anyString(), anyString()))
                .thenReturn(new MockIntegrationResult(CheckOutcome.PASS, "PASS", "Mock passed", Map.of()));

        mvc.perform(get("/api/v1/applications/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));

        JsonNode created = postJson("/api/v1/applications",
                Map.of("country", Country.SWEDEN.name(), "customerType", CustomerType.PRIVATE_INDIVIDUAL.name(),
                        "scenarioKey", "default"));
        String id = created.at("/application/id").asText();

        mvc.perform(put("/api/v1/applications/{id}/steps/identity", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("answers",
                                Map.of("personalNumber", "bad-id")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mvc.perform(post("/api/v1/applications/{id}/agreement", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    static Stream<Arguments> flows() {
        return Stream.of(
                Arguments.of(Country.SWEDEN, CustomerType.PRIVATE_INDIVIDUAL),
                Arguments.of(Country.SPAIN, CustomerType.PRIVATE_INDIVIDUAL),
                Arguments.of(Country.POLAND, CustomerType.PRIVATE_INDIVIDUAL),
                Arguments.of(Country.SWEDEN, CustomerType.BUSINESS),
                Arguments.of(Country.SPAIN, CustomerType.BUSINESS),
                Arguments.of(Country.POLAND, CustomerType.BUSINESS));
    }

    private JsonNode postJson(String path, Object body) throws Exception {
        String json = mvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readJson(json);
    }

    private JsonNode readJson(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON", ex);
        }
    }

    private Map<String, Object> answers(Country country, CustomerType customerType, String stepCode) {
        if (customerType == CustomerType.PRIVATE_INDIVIDUAL) {
            return switch (stepCode) {
                case "identity" -> switch (country) {
                    case SWEDEN -> Map.of("personalNumber", "19940608-1111");
                    case SPAIN -> Map.of("dniNie", "X1234567L");
                    case POLAND -> Map.of("pesel", "44051401458");
                };
                case "contact" -> Map.of("email", "customer@example.com", "phone", "+4612345678", "address", "Main Street 1");
                case "compliance" -> Map.of("consentAccepted", true, "pepDeclaration", "NO", "taxResidency", isoCountry(country));
                case "financial" -> Map.of("employmentStatus", "EMPLOYED", "monthlyIncome", 45000, "monthlyDebt", 5000);
                case "review" -> Map.of("termsAccepted", true);
                default -> throw new IllegalArgumentException("Unsupported private step " + stepCode);
            };
        }
        return switch (stepCode) {
            case "company" -> switch (country) {
                case SWEDEN -> Map.of("organisationNumber", "556016-0680", "legalName", "Example AB", "legalForm", "AB");
                case SPAIN -> Map.of("companyNif", "B12345678", "legalName", "Example SL", "legalForm", "SL");
                case POLAND -> Map.of("companyIdentifier", "1234567890", "legalName", "Example Sp z o.o.", "legalForm", "SP_ZOO");
            };
            case "representative" -> Map.of("representativeName", "Alex Example", "representativeIdentifier", "ID123", "authorityConfirmed", true);
            case "owners" -> Map.of("beneficialOwners", List.of(Map.of("name", "Owner Example", "ownership", 60)));
            case "business" -> Map.of("businessActivity", "Retail", "annualTurnover", 1_200_000, "expectedUsage", "Payments");
            case "decision" -> Map.of("creditConsent", true);
            case "review" -> Map.of("termsAccepted", true);
            default -> throw new IllegalArgumentException("Unsupported business step " + stepCode);
        };
    }

    private String isoCountry(Country country) {
        return switch (country) {
            case SWEDEN -> "SE";
            case SPAIN -> "ES";
            case POLAND -> "PL";
        };
    }
}
