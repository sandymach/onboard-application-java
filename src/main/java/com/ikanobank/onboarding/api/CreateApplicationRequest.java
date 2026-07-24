package com.ikanobank.onboarding.api;

import com.ikanobank.onboarding.domain.*;
import jakarta.validation.constraints.NotNull;

public record CreateApplicationRequest(@NotNull Country country, @NotNull CustomerType customerType,
                                       String scenarioKey) {
}
