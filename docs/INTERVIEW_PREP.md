# Interview prep: Ikano onboarding assignment

## Project summary

This backend implements an adaptive onboarding journey for Sweden, Spain and Poland across private and business customers. It uses Spring Boot, MySQL, Flyway, Temporal, AWS Secrets Manager/RDS, local JSON mocks, structured logging and Postman scenarios.

The important design point is separation of concerns:

- flow configuration decides which steps/checks are required;
- service layer owns state transitions and validation;
- integrations are deterministic local JSON mocks;
- Temporal tracks resumable orchestration;
- MySQL stores production-shaped domain tables;
- AWS Secrets Manager supplies datasource credentials.

## Likely interview questions and strong answer points

### 1. Why did you use configurable flows?

Because onboarding differs by country and customer type. Hardcoding six flows would duplicate business logic and make maintenance risky. YAML flow definitions keep market variation in configuration while the service layer enforces common rules.

### 2. How would you avoid creating N Temporal workflows in production?

Use one product-level workflow per product line, then reusable workflows for common sub-journeys such as IDV, agreement/signing and account setup. Country/customer differences should be flow data or strategy components, not separate workflow classes for every combination.

### 3. What is the purpose of Temporal here?

Temporal gives durable orchestration and visibility for long-running onboarding journeys. It tracks state across REST calls and can recover from worker restarts. The current implementation keeps business decisions in services and uses Temporal for orchestration visibility.

### 4. Why did some workflows remain running?

Approved journeys should run until account setup completes. Referred journeys intentionally remain running until manual review. Declined/cancelled journeys should complete immediately. The fix was to send a terminal Temporal signal for `DECLINED` and manual `CANCELLED`/`DECLINED`.

### 5. How is manual review executed?

Applications with `MANUAL_REVIEW` can be overridden through:

```text
POST /api/v1/applications/{id}/manual-override
```

Allowed target states:

- `AGREEMENT_CREATED`: manual approval, then continue agreement/signing/account setup.
- `DECLINED`: terminal decline.
- `CANCELLED`: terminal cancellation.

### 6. Why is `APPROVED` the final successful state?

Because a positive decision is not the end of onboarding. The customer still needs agreement creation, signing and account setup. So positive decision moves to `AGREEMENT_CREATED`; final success after account setup becomes `APPROVED`.

### 7. How is traceability handled?

Each request can carry:

- `X-Transaction-Id`
- `X-Trace-Id`
- `X-Request-Id`
- product/country/channel headers

The request filter writes these into MDC. `logback.xml` emits them in every log line so CloudWatch can be queried by transaction/application ID.

### 8. Why local JSON mocks instead of S3/localstack?

The assignment needs deterministic mocked integrations, not object storage behavior. Local JSON files are simpler, faster and easier to run in an interview/demo. AWS is kept where it matters: RDS and Secrets Manager.

### 9. What is the production database shape?

The schema uses explicit domain tables:

- `application`
- `application_events`
- `applicant`
- `customer`
- `idv`
- `decision`
- `agreement`
- `signing`
- `step_result`
- `integration_result`

Each table uses explicit IDs such as `application_id`, `applicant_id`, `decision_id`, and foreign keys back to `application.application_id`.

### 10. Why keep `step_result` and `integration_result`?

They are diagnostic/supporting tables. `step_result` stores submitted answers and fingerprints. `integration_result` stores raw mock check outcomes. Domain tables such as `idv`, `decision`, `agreement` and `signing` provide business-oriented views.

### 11. How are secrets handled?

Runtime datasource values are read from AWS Secrets Manager using `IKANOBANK_DB_SECRET_ID=application-db`. DB username/password are not required in IntelliJ runtime env vars.

### 12. How would you improve this further?

Good follow-ups:

- authentication/authorization;
- PII encryption and retention policies;
- richer audit event model;
- idempotency keys for external callbacks;
- real provider integrations behind interfaces;
- infrastructure-as-code instead of shell scripts;
- Temporal activities with retries/timeouts for real external calls;
- contract tests for Postman/OpenAPI scenarios;
- separate read models for operations/manual review.

## Common pitfalls to explain clearly

- Do not edit already-applied Flyway migrations in shared databases; add a new migration.
- Temporal workflow code must be deterministic.
- Referred/manual-review workflows are supposed to wait until override.
- Declined/cancelled workflows must be completed explicitly.
- `APPROVED` should not be set before signing/account setup.

## Demo flow to present

1. Start MySQL, Temporal and Temporal UI.
2. Start Spring Boot with AWS Secrets Manager DB secret.
3. Run Postman approved scenario.
4. Show `application` row and related `decision/agreement/signing` rows.
5. Show Temporal workflow completion.
6. Run manual-review scenario.
7. Show it waiting in `MANUAL_REVIEW`.
8. Execute manual override to `AGREEMENT_CREATED` or `DECLINED`.
9. Show workflow outcome.
