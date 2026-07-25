package com.ikanobank.onboarding.flow;

import java.util.List;

public record FlowStepDefinition(int order, String code, String title, List<String> integrations) {
}
