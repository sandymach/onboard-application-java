package com.ikanobank.onboarding.orchestration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

@Configuration
@EnableConfigurationProperties(TemporalProperties.class)
public class TemporalConfig {
    @Bean
    @ConditionalOnProperty(prefix = "app.temporal", name = "enabled", havingValue = "true")
    WorkflowServiceStubs workflowServiceStubs(TemporalProperties properties) {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(properties.target())
                        .build());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.temporal", name = "enabled", havingValue = "true")
    WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs, TemporalProperties properties) {
        return WorkflowClient.newInstance(serviceStubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(properties.namespace())
                        .build());
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "app.temporal", name = "enabled", havingValue = "true")
    WorkerFactory workerFactory(WorkflowClient workflowClient, TemporalProperties properties) {
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
        Worker worker = factory.newWorker(properties.taskQueue());
        worker.registerWorkflowImplementationTypes(OnboardingWorkflowImpl.class,
                IdvJourneyWorkflowImpl.class,
                AgreementSigningWorkflowImpl.class);
        factory.start();
        return factory;
    }
}
