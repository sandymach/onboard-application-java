# Python reimplementation context

Use this document as the blueprint for rebuilding this backend in Python with AWS RDS MySQL and Temporal.

## Goal

Create a Python backend equivalent to the current Java/Spring Boot onboarding backend.

Recommended stack:

- FastAPI
- OpenAPI-first design with the contract stored under `resources/specifications/`
- SQLAlchemy 2.x
- Alembic
- MySQL
- Pydantic
- Temporal Python SDK
- boto3 for AWS Secrets Manager
- pytest
- Docker Compose for MySQL, Temporal and Temporal UI

Target deployment shape:

- application database on AWS RDS MySQL
- datasource credentials in AWS Secrets Manager
- Temporal cluster for durable workflow state
- Temporal UI for local and demo visibility
- cloud logs emitted to stdout/stderr

## Functional scope

The service supports onboarding journeys for:

- Sweden private
- Sweden business
- Spain private
- Spain business
- Poland private
- Poland business

The API should expose equivalent endpoints:

```text
GET  /api/v1/flows
GET  /api/v1/flows/{country}/{type}
POST /api/v1/applications
POST /api/v1/applications/resume
GET  /api/v1/applications/{id}
PUT  /api/v1/applications/{id}/steps/{stepCode}
POST /api/v1/applications/{id}/checks
POST /api/v1/applications/{id}/submit
POST /api/v1/applications/{id}/manual-override
POST /api/v1/applications/{id}/agreement
POST /api/v1/applications/{id}/agreement/sign-later
POST /api/v1/applications/{id}/agreement/sign
POST /api/v1/applications/{id}/account-setup
GET  /api/v1/applications/{id}/audit-events
```

Start from an OpenAPI YAML contract first. Generate Pydantic request/response models or keep generated schemas aligned with the contract before implementing route handlers.

For the web requirement, include a server-rendered page:

```text
GET /onboarding
```

It should match the current Java UI in behavior and structure as closely as practical:

- a first setup screen with product, country, customer type and scenario selection;
- a guided onboarding journey screen;
- step-by-step form progression;
- mocked check execution and visible technical response output;
- decision/status feedback;
- agreement creation, sign-later, sign-now and account setup actions;
- manual review override controls;
- audit/status panels;
- the same overall customer-facing flow and interaction model as the current Java version.

The Python version does not need pixel-perfect styling, but it should preserve the same functional UI contract, button flow, and user journey so the backend behavior feels the same.

## Runtime configuration

Runtime env vars:

```text
AWS_REGION=eu-north-1
AWS_ACCESS_KEY_ID=<key>
AWS_SECRET_ACCESS_KEY=<secret>
AWS_SESSION_TOKEN=<only for temp creds>
IKANOBANK_DB_SECRET_ID=application-db
IKANOBANK_MOCK_DATA_PATH=mock-data
TEMPORAL_ENABLED=true
TEMPORAL_TARGET=localhost:7233
TEMPORAL_NAMESPACE=default
TEMPORAL_TASK_QUEUE=onboarding
```

If `IKANOBANK_DB_SECRET_ID` is set, read JSON from AWS Secrets Manager:

```json
{
  "url": "jdbc:mysql://<host>:3306/ikanobank-onbording-db?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC",
  "username": "admin",
  "password": "admin"
}
```

Convert JDBC URL into SQLAlchemy URL:

```text
mysql+pymysql://admin:admin@<host>:3306/ikanobank-onbording-db
```

The Python app should not require `SPRING_DATASOURCE_*`; instead, the database URL is resolved from Secrets Manager at startup.

Suggested Secrets Manager payload shape:

```json
{
  "url": "jdbc:mysql://<rds-endpoint>:3306/<db-name>?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC",
  "username": "<db-user>",
  "password": "<db-password>"
}
```

## Domain statuses

Use these application statuses:

```text
CREATED
INITIATED
IN_PROGRESS
KYC
IDV
READY_FOR_REVIEW
MANUAL_REVIEW
DECLINED
AGREEMENT_CREATED
SIGNING_PENDING
AGREEMENT_SIGNED
ACCOUNT_SETUP_COMPLETE
APPROVED
CANCELLED
EXPIRED
ABANDONED
```

Rules:

- Create application -> `INITIATED`.
- Step submission moves through `IN_PROGRESS`, `KYC`, `IDV`.
- Last step -> `READY_FOR_REVIEW`.
- Checks must run before submit.
- Submit:
  - any `FAIL` -> `DECLINED`;
  - any `MANUAL_REVIEW` or `UNAVAILABLE` -> `MANUAL_REVIEW`;
  - all pass -> `AGREEMENT_CREATED`.
- Agreement creation keeps `AGREEMENT_CREATED`.
- Sign later -> `SIGNING_PENDING`.
- Sign now -> `AGREEMENT_SIGNED`.
- Account setup -> `APPROVED`.
- Manual override only from `MANUAL_REVIEW` to:
  - `AGREEMENT_CREATED`;
  - `DECLINED`;
  - `CANCELLED`.

Important implementation detail:

- `AGREEMENT_CREATED` is not terminal. It means the customer can continue into agreement creation, signing and account setup.
- `MOCK_SERVICE_UNAVAILABLE` should only appear when a mock integration payload cannot be loaded or the mock service layer is unavailable.
- `APPROVED` is only reached after account setup succeeds.

## Database schema

Create Alembic migrations for:

- `application`
  - `application_id` primary key
  - country, customer_type, current_step_code, status, scenario_key
  - resume_token_hash, resume_token_expires_at, expires_at
  - decision_reason, created_at, updated_at, submitted_at
- `application_events`
  - `application_event_id` primary key
  - `application_id` foreign key
  - event_type, result_code, request_id, occurred_at
- `applicant`
  - `applicant_id` primary key
  - `application_id` foreign key
  - applicant_type, country, created_at, updated_at
- `customer`
  - `customer_id` primary key
  - `application_id` foreign key
  - customer_type, country, status, created_at, updated_at
- `idv`
  - `idv_id` primary key
  - `application_id` foreign key
  - `applicant_id` nullable foreign key
  - outcome, provider, reason_code, message, request_id, checked_at
- `decision`
  - `decision_id` primary key
  - `application_id` foreign key
  - decision_status, reason_code, decision_source, decided_by, request_id, decided_at
- `agreement`
  - `agreement_id` primary key
  - `application_id` foreign key
  - agreement_reference, outcome, reason_code, message, created_at
- `signing`
  - `signing_id` primary key
  - `application_id` foreign key
  - signing_reference, signing_mode, outcome, reason_code, message, signed_at
- `step_result`
  - `step_result_id` primary key
  - `application_id` foreign key
  - step_code, answers_json, answers_fingerprint, completed_at
  - unique `(application_id, step_code)`
- `integration_result`
  - `integration_result_id` primary key
  - `application_id` foreign key
  - integration_type, outcome, reason_code, message, answers_fingerprint, request_id, checked_at

Use MySQL-compatible column types and explicit foreign keys. Preserve readable UUID identifiers as `CHAR(36)` for application-level records and use short human-readable string IDs for applicant/customer technical projections if you want parity with the Java implementation.

## Flow handling

Keep flow definitions data-driven. Either:

- read the existing YAML files from `src/main/resources/flows`, or
- port them into Python `resources/flows/*.yml`.

Flow selection key:

```text
country + customerType
```

Each step has:

- code
- order
- title
- required fields
- required integration types

The current backend has six supported journeys:

- Sweden private
- Sweden business
- Spain private
- Spain business
- Poland private
- Poland business

The Python rewrite should preserve the same six combinations unless the business scope changes.

## Mock integrations

Read deterministic JSON files from `mock-data/`.

Integration types include:

- identity
- address
- sanctions
- pep
- credit
- registry
- bank-account
- agreement
- signing
- account-setup

Scenario key comes from application creation. If missing, use `default`.

The Java implementation loads mock files from both filesystem and classpath so the app works locally and in a packaged deployment. The Python version should do the same or package the mock fixtures into the container image.

## Temporal behavior

Use Temporal Python SDK with:

- product workflow: `OnboardingWorkflow`
- reusable workflow: `IdvJourneyWorkflow`
- reusable workflow: `AgreementSigningWorkflow`

Recommended behavior:

- start `OnboardingWorkflow` on application create with workflow ID `onboarding-{application_id}`;
- signal `step_completed` on step submit;
- signal `checks_requested` on checks;
- start separate reusable IDV workflow with ID `idv-{application_id}`;
- signal `agreement_created` and start agreement/signing workflow with ID `agreement-signing-{application_id}`;
- signal `agreement_signed` on signing;
- signal `account_setup_completed` and complete product workflow on final approval;
- signal terminal completion for `DECLINED` and `CANCELLED`;
- keep `MANUAL_REVIEW` running until manual override.

Temporal should be treated as durable orchestration and visibility, not as the place where all domain rules live. Keep decision logic and database writes in service/activity layers so workflow code stays deterministic.

Recommended workflow IDs:

- `onboarding-{application_id}`
- `idv-{application_id}`
- `agreement-signing-{application_id}`

Avoid nondeterministic workflow code. Keep DB/network calls in activities or outside workflow code.

## Logging and observability

Add request middleware that extracts/generates:

- `X-Request-Id`
- `X-Trace-Id`
- `X-Transaction-Id`
- `X-Product-Code`
- `X-Country`
- `X-Channel`

Log these fields on every request. In AWS, publish stdout logs to CloudWatch.

## Testing

Add pytest coverage for:

- create/resume;
- step order enforcement;
- validation failure;
- approved journey through account setup;
- declined journey terminal completion;
- manual review and manual override;
- sign-later;
- schema constraints;
- deterministic mock outcomes.

Also include startup tests that prove:

- AWS Secrets Manager credentials are read correctly;
- RDS MySQL connection settings are accepted;
- Temporal can be disabled for normal unit tests and enabled for integration tests.

## Existing support assets

Reuse from this repository:

- `mock-data/`
- `docs/postman/Ikano_Onboarding.postman_collection.json`
- `docs/postman/Ikano_Onboarding.scenarios.postman_data.json`
- `docker-compose.yml` service ideas
- `scripts/aws/create-resources.sh`
- `scripts/aws/destroy-resources.sh`

## Reimplementation notes

The current Java application is structured around a few stable boundaries that should be preserved in Python:

- API/controller layer: request validation, response shaping, HTTP status mapping.
- Service layer: state transitions, step validation, lifecycle rules, submit/override logic.
- Integration layer: deterministic mock check execution.
- Flow layer: country/type-specific step definitions.
- Persistence layer: application, audit, step result, integration result and domain tables.
- Temporal layer: resumable orchestration and long-running journey state.

If you keep those boundaries intact, the Python version will stay testable and easy to reason about.
