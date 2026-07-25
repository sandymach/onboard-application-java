package com.ikanobank.onboarding.flow;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.ikanobank.onboarding.domain.Country;
import com.ikanobank.onboarding.domain.CustomerType;

@Component
public class FlowRegistry {
    private static final Logger log = LoggerFactory.getLogger(FlowRegistry.class);
    private final Map<String, FlowDefinition> flows = new HashMap<>();

    public FlowRegistry() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:flows/*.yml");
        for (Resource r : resources) {
            FlowDefinition f = mapper.readValue(r.getInputStream(), FlowDefinition.class);
            validate(f, r.getFilename());
            flows.put(key(f.country(), f.customerType()), f);
            log.info("Loaded flow country={} customerType={} steps={}", f.country(), f.customerType(), f.steps().size());
        }
    }

    public FlowDefinition get(Country c, CustomerType t) {
        FlowDefinition f = flows.get(key(c, t));
        if (f == null) throw new IllegalArgumentException("Unsupported flow: " + c + "/" + t);
        return f;
    }

    public Collection<FlowDefinition> all() {
        return List.copyOf(flows.values());
    }

    private String key(Country c, CustomerType t) {
        return c + ":" + t;
    }

    private void validate(FlowDefinition flow, String source) {
        if (flow.country() == null || flow.customerType() == null || flow.steps() == null || flow.steps().isEmpty()) {
            throw new IllegalArgumentException("Invalid flow definition: " + source);
        }
        Set<String> codes = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        for (FlowStepDefinition step : flow.steps()) {
            if (step.order() < 1 || step.code() == null || step.code().isBlank() || step.title() == null || step.title().isBlank()) {
                throw new IllegalArgumentException("Invalid step in flow definition: " + source);
            }
            if (!codes.add(step.code())) {
                throw new IllegalArgumentException("Duplicate step code " + step.code() + " in " + source);
            }
            if (!orders.add(step.order())) {
                throw new IllegalArgumentException("Duplicate step order " + step.order() + " in " + source);
            }
            if (step.integrations() == null) {
                throw new IllegalArgumentException("Step integrations must be an empty list, not null: " + source);
            }
        }
    }
}
