package com.ikanobank.onboarding.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ikanobank.onboarding.integration.*;

@Configuration
public class IntegrationClientConfig {
    @Bean
    List<MockIntegrationClient> mockIntegrationClients(LocalScenarioRepository r) {
        return List.of(
                new LocalFileMockClient("identity", r),
                new LocalFileMockClient("sanctions", r),
                new LocalFileMockClient("credit", r),
                new LocalFileMockClient("registry", r),
                new LocalFileMockClient("bank-account", r),
                new LocalFileMockClient("address", r),
                new LocalFileMockClient("agreement", r),
                new LocalFileMockClient("signing", r),
                new LocalFileMockClient("account-setup", r));
    }
}
