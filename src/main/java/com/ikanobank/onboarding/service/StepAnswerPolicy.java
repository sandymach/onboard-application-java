package com.ikanobank.onboarding.service;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.util.*;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikanobank.onboarding.domain.Country;
import com.ikanobank.onboarding.domain.CustomerType;

@Component
public class StepAnswerPolicy {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern SWEDISH_PERSONAL_NUMBER = Pattern.compile("^(19|20)?\\d{6}[-+]?\\d{4}$");
    private static final Pattern SPANISH_DNI_NIE = Pattern.compile("^[XYZ]?\\d{7,8}[A-Z]$");
    private static final Pattern POLISH_PESEL = Pattern.compile("^\\d{11}$");
    private static final Pattern SWEDISH_ORG_NUMBER = Pattern.compile("^\\d{6}-?\\d{4}$");
    private static final Pattern SPANISH_COMPANY_NIF = Pattern.compile("^[A-Z]\\d{8}$");
    private static final Pattern POLISH_COMPANY_ID = Pattern.compile("^(PL)?\\d{10}$");
    private static final int MIN_PRIVATE_APPLICANT_AGE = 18;
    private static final int MIN_MONTHLY_INCOME = 10_000;
    private static final int MAX_DEBT_TO_INCOME_PERCENT = 60;

    private final ObjectMapper mapper;

    public StepAnswerPolicy(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void validate(Country country, CustomerType customerType, String stepCode, Map<String, Object> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("Step answers are required");
        }
        for (String field : requiredFields(country, customerType, stepCode)) {
            Object value = answers.get(field);
            if (value == null || (value instanceof String s && s.isBlank())) {
                throw new IllegalArgumentException("Missing required field: " + field);
            }
        }
        validateFieldRules(country, customerType, stepCode, answers);
    }

    public String fingerprint(Map<String, Object> answers) {
        try {
            byte[] json = mapper.writeValueAsBytes(new TreeMap<>(answers));
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(json);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid step answers", ex);
        }
    }

    public Set<String> requiredFields(Country country, CustomerType customerType, String stepCode) {
        if (customerType == CustomerType.PRIVATE_INDIVIDUAL) {
            return switch (stepCode) {
                case "identity" -> switch (country) {
                    case SWEDEN -> Set.of("personalNumber");
                    case SPAIN -> Set.of("dniNie");
                    case POLAND -> Set.of("pesel");
                };
                case "contact" -> Set.of("email", "phone", "address");
                case "compliance" -> Set.of("consentAccepted", "pepDeclaration", "taxResidency");
                case "financial" -> Set.of("employmentStatus", "monthlyIncome", "monthlyDebt");
                case "review" -> Set.of("termsAccepted");
                default -> Set.of();
            };
        }
        return switch (stepCode) {
            case "company" -> switch (country) {
                case SWEDEN -> Set.of("organisationNumber", "legalName", "legalForm");
                case SPAIN -> Set.of("companyNif", "legalName", "legalForm");
                case POLAND -> Set.of("companyIdentifier", "legalName", "legalForm");
            };
            case "representative" -> Set.of("representativeName", "representativeIdentifier", "authorityConfirmed");
            case "owners" -> Set.of("beneficialOwners");
            case "business" -> Set.of("businessActivity", "annualTurnover", "expectedUsage");
            case "decision" -> Set.of("creditConsent");
            case "review" -> Set.of("termsAccepted");
            default -> Set.of();
        };
    }

    private void validateFieldRules(Country country, CustomerType customerType, String stepCode, Map<String, Object> answers) {
        if (customerType == CustomerType.PRIVATE_INDIVIDUAL) {
            validatePrivateRules(country, stepCode, answers);
        } else {
            validateBusinessRules(country, stepCode, answers);
        }
    }

    private void validatePrivateRules(Country country, String stepCode, Map<String, Object> answers) {
        switch (stepCode) {
            case "identity" -> validatePrivateIdentity(country, answers);
            case "contact" -> {
                requirePattern("email", text(answers, "email"), EMAIL);
                requireMinLength("address", text(answers, "address"), 5);
                requireMinLength("phone", text(answers, "phone"), 7);
            }
            case "compliance" -> {
                requireTrue("consentAccepted", answers.get("consentAccepted"));
                requirePattern("taxResidency", text(answers, "taxResidency"), Pattern.compile("^[A-Z]{2}$"));
            }
            case "financial" -> {
                int income = requireInteger("monthlyIncome", answers.get("monthlyIncome"));
                int debt = requireInteger("monthlyDebt", answers.get("monthlyDebt"));
                if (income < MIN_MONTHLY_INCOME) {
                    throw new IllegalArgumentException("monthlyIncome must be at least " + MIN_MONTHLY_INCOME);
                }
                if (debt < 0) {
                    throw new IllegalArgumentException("monthlyDebt cannot be negative");
                }
                if (debt * 100 > income * MAX_DEBT_TO_INCOME_PERCENT) {
                    throw new IllegalArgumentException("monthlyDebt is too high compared with monthlyIncome");
                }
            }
            case "review" -> requireTrue("termsAccepted", answers.get("termsAccepted"));
            default -> {
            }
        }
    }

    private void validatePrivateIdentity(Country country, Map<String, Object> answers) {
        switch (country) {
            case SWEDEN -> {
                String value = text(answers, "personalNumber");
                requirePattern("personalNumber", value, SWEDISH_PERSONAL_NUMBER);
                requireMinimumAge("personalNumber", value.substring(value.length() == 13 ? 0 : 0, value.length()), country);
            }
            case SPAIN -> requirePattern("dniNie", text(answers, "dniNie"), SPANISH_DNI_NIE);
            case POLAND -> {
                String value = text(answers, "pesel");
                requirePattern("pesel", value, POLISH_PESEL);
                requireMinimumAge("pesel", value, country);
            }
        }
    }

    private void validateBusinessRules(Country country, String stepCode, Map<String, Object> answers) {
        switch (stepCode) {
            case "company" -> {
                switch (country) {
                    case SWEDEN -> requirePattern("organisationNumber", text(answers, "organisationNumber"), SWEDISH_ORG_NUMBER);
                    case SPAIN -> requirePattern("companyNif", text(answers, "companyNif"), SPANISH_COMPANY_NIF);
                    case POLAND -> requirePattern("companyIdentifier", text(answers, "companyIdentifier"), POLISH_COMPANY_ID);
                }
                requireMinLength("legalName", text(answers, "legalName"), 2);
            }
            case "representative" -> {
                requireMinLength("representativeName", text(answers, "representativeName"), 2);
                requireTrue("authorityConfirmed", answers.get("authorityConfirmed"));
            }
            case "owners" -> requireMinLength("beneficialOwners", text(answers, "beneficialOwners"), 3);
            case "business" -> {
                requireMinLength("businessActivity", text(answers, "businessActivity"), 3);
                int annualTurnover = requireInteger("annualTurnover", answers.get("annualTurnover"));
                if (annualTurnover < 1) {
                    throw new IllegalArgumentException("annualTurnover must be positive");
                }
            }
            case "decision", "review" -> requireTrue(stepCode.equals("decision") ? "creditConsent" : "termsAccepted",
                    answers.get(stepCode.equals("decision") ? "creditConsent" : "termsAccepted"));
            default -> {
            }
        }
    }

    private void requireMinimumAge(String field, String value, Country country) {
        LocalDate birthDate = switch (country) {
            case SWEDEN -> birthDateFromSwedishPersonalNumber(value);
            case POLAND -> birthDateFromPesel(value);
            case SPAIN -> null;
        };
        if (birthDate != null && Period.between(birthDate, LocalDate.now()).getYears() < MIN_PRIVATE_APPLICANT_AGE) {
            throw new IllegalArgumentException(field + " indicates applicant must be at least " + MIN_PRIVATE_APPLICANT_AGE);
        }
    }

    private LocalDate birthDateFromSwedishPersonalNumber(String value) {
        String digits = value.replaceAll("\\D", "");
        String datePart = digits.length() == 12 ? digits.substring(0, 8) : String.valueOf(resolveTwoDigitYear(Integer.parseInt(digits.substring(0, 2)))) + digits.substring(2, 6);
        return LocalDate.of(Integer.parseInt(datePart.substring(0, 4)), Integer.parseInt(datePart.substring(4, 6)), Integer.parseInt(datePart.substring(6, 8)));
    }

    private LocalDate birthDateFromPesel(String value) {
        int year = Integer.parseInt(value.substring(0, 2));
        int encodedMonth = Integer.parseInt(value.substring(2, 4));
        int day = Integer.parseInt(value.substring(4, 6));
        int century = encodedMonth >= 21 ? 2000 : 1900;
        int month = encodedMonth >= 21 ? encodedMonth - 20 : encodedMonth;
        return LocalDate.of(century + year, month, day);
    }

    private int resolveTwoDigitYear(int year) {
        int currentTwoDigitYear = Year.now().getValue() % 100;
        return year <= currentTwoDigitYear ? 2000 + year : 1900 + year;
    }

    private void requirePattern(String field, String value, Pattern pattern) {
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid " + field);
        }
    }

    private void requireMinLength(String field, String value, int minLength) {
        if (value.length() < minLength) {
            throw new IllegalArgumentException(field + " must be at least " + minLength + " characters");
        }
    }

    private void requireTrue(String field, Object value) {
        if (!(value instanceof Boolean b && b) && !(value instanceof String s && Boolean.parseBoolean(s))) {
            throw new IllegalArgumentException(field + " must be accepted");
        }
    }

    private int requireInteger(String field, Object value) {
        try {
            if (value instanceof Number n) {
                return n.intValue();
            }
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            throw new IllegalArgumentException(field + " must be a number");
        }
    }

    private String text(Map<String, Object> answers, String field) {
        Object value = answers.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }
}
