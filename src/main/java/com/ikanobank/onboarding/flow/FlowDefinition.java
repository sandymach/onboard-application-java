package com.ikanobank.onboarding.flow;

import java.util.List;

import com.ikanobank.onboarding.domain.Country;
import com.ikanobank.onboarding.domain.CustomerType;

public record FlowDefinition(Country country, CustomerType customerType, List<FlowStepDefinition> steps) {
}
