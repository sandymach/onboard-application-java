# Architecture

## Backend shape

The service is a Spring Boot web/backend application. It exposes a server-rendered onboarding journey at `/onboarding` and REST APIs under `/api/v1`.

Main boundaries:

- Web layer: minimal server-rendered onboarding page at `/onboarding` for the assignment demo.
- API layer: `OnboardingController` implements the Spring API interfaces generated from OpenAPI. JPA entities are not exposed from the public API.
- Contract layer: OpenAPI YAML under `src/main/resources/specifications`; Gradle validates the contract and generates Spring API interfaces/models under `build/generated/openapi`.
- Flow engine: YAML-backed flow definitions for country and customer type.
- Application service: state transitions, resume behavior, validation, integration execution and final submission.
- Integration layer: local JSON deterministic mock clients with explicit pass/manual/fail/unavailable outcomes.
- Decisioning layer: converts integration results into approved, manual review or rejected.
- Fulfilment layer: creates agreements, supports sign-now/sign-later and runs account setup after approval.
- Audit layer: records customer and system events with request IDs and without raw sensitive answers in logs.
- Orchestration layer: Temporal product workflow plus reusable IDV and agreement/signing workflow types model resumable onboarding state.

## Data model

Core persisted records:

- `application`: selected country/type, current step, status, scenario key, resume token hash, expiry, decision and timestamps. Primary key: `application_id`.
- `application_events`: immutable event/audit trail. Primary key: `application_event_id`; foreign key to `application.application_id`.
- `applicant`: applicant-level onboarding snapshot. Primary key: `applicant_id`; foreign key to `application.application_id`.
- `customer`: customer projection for the journey. Primary key: `customer_id`; foreign key to `application.application_id`.
- `idv`: identity/PEP/sanctions results. Primary key: `idv_id`; foreign keys to `application.application_id` and `applicant.applicant_id`.
- `decision`: system and manual decision history. Primary key: `decision_id`; foreign key to `application.application_id`.
- `agreement`: agreement creation result/reference. Primary key: `agreement_id`; foreign key to `application.application_id`.
- `signing`: sign-now/sign-later result/reference. Primary key: `signing_id`; foreign key to `application.application_id`.
- `step_result`: completed step answers as JSON plus a fingerprint used to detect changed inputs. Primary key: `step_result_id`; foreign key to `application.application_id`.
- `integration_result`: raw mock external check outcome, reason, request ID and answer fingerprint retained for diagnostics. Primary key: `integration_result_id`; foreign key to `application.application_id`.

The Java API still exposes `id` as the DTO field for compatibility, but the physical database schema uses explicit domain identifiers such as `application_id`, `applicant_id`, `customer_id`, `decision_id`, `agreement_id` and `signing_id`.

Application and technical event IDs are stored as readable UUID `char(36)` values, not `binary(16)`, so MySQL Workbench and support queries show normal UUID strings.

Applicant and customer IDs are intentionally shorter business-readable numeric strings:

- `applicant.applicant_id`: `char(10)`, digits only.
- `customer.customer_id`: `char(10)`, digits only.
- `idv.applicant_id`: `char(10)` foreign key.

The demo generation rule derives these IDs from the application UUID into a 10-digit numeric namespace: applicant IDs start with `1`, customer IDs start with `2`, and later applicant snapshots start with `3`.

Customer lifecycle status is explicit:

- `NEW_CUSTOMER` when an onboarding application is created.
- `EXISTING_CUSTOMER` after agreement signing and successful account setup.

MySQL profile uses Flyway migrations and is the supported local database path.

## Flow configuration

Flows live in `src/main/resources/flows/*.yml`. A flow has a country, customer type and ordered steps. Each step declares the mock integration types that must be run before final submission.

Adding a new market should require:

1. Add enum value if needed.
2. Add a YAML flow.
3. Add country-specific validation only where the input differs.
4. Add mock payloads when new scenarios are needed.

## Mock integration strategy

Local development reads deterministic mock integration responses from `mock-data/`.

The AWS integration retained for local development is Secrets Manager, used for datasource credentials.

## Temporal

Temporal is included for resumability and orchestration. The workflow state tracks application ID, last completed step and check request state. Business rules stay in services so tests can validate behavior without a Temporal server.

The Docker Compose file includes Temporal and Temporal UI for local inspection. The Spring application registers the Temporal worker when `TEMPORAL_ENABLED=true`; otherwise the workflow types remain available to the codebase without requiring a Temporal server during normal tests and local smoke runs.

The intended production shape is one product workflow per product line, with reusable workflow types for common journeys such as IDV, agreement creation/signing and account setup. This avoids creating a separate full workflow for every country/customer-type combination.

## Production notes

This is still a take-home backend, not a regulated production system. Remaining production work would include authentication, authorization, secrets management, rate limiting, richer observability, data retention policies, PII encryption strategy, deployment infrastructure and operational runbooks.

The controller implements the generated API interfaces. The service layer still uses small internal DTO records so business logic is decoupled from generated source; the controller maps between generated public models and internal service models.
