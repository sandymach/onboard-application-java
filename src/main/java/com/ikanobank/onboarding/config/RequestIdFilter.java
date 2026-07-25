package com.ikanobank.onboarding.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Request-Id";
    public static final String TRANSACTION_HEADER = "X-Transaction-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String id = req.getHeader(HEADER);
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        String traceId = firstNonBlank(req.getHeader("X-Trace-Id"), req.getHeader("X-B3-TraceId"), id);
        MDC.put("traceId", traceId);
        putIfPresent("productCode", req.getHeader("X-Product-Code"));
        putIfPresent("country", req.getHeader("X-Country"));
        putIfPresent("channel", req.getHeader("X-Channel"));
        res.setHeader(HEADER, id);
        res.setHeader("X-Trace-Id", traceId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("traceId");
            MDC.remove("applicationId");
            MDC.remove("productCode");
            MDC.remove("country");
            MDC.remove("customerType");
            MDC.remove("channel");
        }
    }

    private void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, sanitizeMdcValue(value));
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return sanitizeMdcValue(value);
            }
        }
        return UUID.randomUUID().toString();
    }

    private String sanitizeMdcValue(String value) {
        return value.replaceAll("[^A-Za-z0-9._:/@-]", "_")
                .substring(0, Math.min(value.length(), 128));
    }
}
