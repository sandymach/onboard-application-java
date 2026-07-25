# Evaluation coverage

This document maps the final implementation to the assignment requirements and evaluation rubric.

## Core requirements

| Requirement | Coverage |
|---|---|
| Web onboarding app | `/onboarding` provides a customer-facing Ikano Bank onboarding journey. |
| Country selector | First screen supports Sweden, Spain and Poland. |
| Account-type selector | First screen supports private individual and business. |
| One of six flows | YAML flow definitions cover country + customer type combinations. |
| Meaningful validation | `StepAnswerPolicy` enforces required fields by country, customer type and step. |
| Progress and feedback | Web UI shows progress pills, current state, next action and policy notes. |
| Mock KYC/KYB/sanctions/credit/registry checks | Local JSON mocks under `mock-data/` cover identity, address, sanctions, credit, registry and bank account checks. |
| Deterministic pass/manual/fail outcomes | Scenario keys drive explicit `PASS`, `MANUAL_REVIEW` and `FAIL` results. |
| Decision outcome | Decision service maps checks to `AGREEMENT_CREATED`, `MANUAL_REVIEW` or `DECLINED`; final successful state becomes `APPROVED` after account setup. |
| Audit trail | `application_events` and `/audit-events` expose checked events and lifecycle transitions. |
| Persistence | MySQL/Flyway schema persists application, applicant, customer, IDV, decision, agreement, signing, step results, integration results and audit events. |
| README/runnable locally/tests | README, RUNBOOK, Postman collection and Gradle tests are provided. |

## Evaluation rubric

| Area | Weight | Evidence |
|---|---:|---|
| Product flow and UX | 15% | `/onboarding` is a guided customer journey with product/country/applicant selection, filtered mock scenarios, pre-filled forms, validation/error demo buttons, progress, review/decision feedback, agreement preview and click-to-sign checkbox. |
| Architecture and data model | 20% | Clear API/service/flow/integration/decision/audit/orchestration boundaries; explicit production-style tables with readable application UUIDs, short numeric applicant/customer IDs and foreign keys. |
| Mocks and decisioning | 20% | Local deterministic mock clients; scenario keys for approve/manual-review/decline; policy notes for underwriting, AML, Fraud/KYC and KYB. |
| Code quality and tests | 20% | Service-oriented implementation, flow-driven configuration, controller/API separation, OpenAPI contract validation/code generation, integration tests and unit tests for decisioning, validation, resume tokens, flows and Temporal state. |
| Production mindset | 15% | AWS Secrets Manager for DB credentials, RDS provisioning scripts, Flyway migrations, readable UUID IDs, audit events, MDC logging, CloudWatch-ready stdout pattern, no sensitive MDC values, recovery scripts and standard 400/403/404/409/500 error responses. |
| Communication | 10% | README, RUNBOOK, ARCHITECTURE, ASSUMPTIONS, INTERVIEW_PREP and Python reimplementation context document tradeoffs and next steps. |

## Explicit tradeoffs

- The assignment brief asks for Python; this delivery remains Java/Spring Boot. A Python implementation handoff document is provided in `docs/PYTHON_REIMPLEMENTATION_CONTEXT.md`.
- The web UI is intentionally lightweight and server-rendered. It prioritizes product flow clarity over visual polish.
- Mock integrations are local JSON files, not real banking/provider integrations.
- Temporal is used for orchestration visibility and resumability, while business rules remain in the service layer for readability and testability.
- Contract-first is represented by `src/main/resources/specifications/onboarding-api.yaml`; full generated API interfaces/models are listed as a next production step.
