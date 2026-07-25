# Ikano Onboarding Backend

Java 17 / Spring Boot backend for an adaptive Ikano-style onboarding journey across Sweden, Spain and Poland for private individuals and businesses.

The assignment brief asks for Python. This repository is implemented in Java/Spring Boot for this delivery, with a dedicated Python reimplementation context in `docs/PYTHON_REIMPLEMENTATION_CONTEXT.md`.

## What it does

- Loads six configurable flows from YAML.
- Provides a minimal server-rendered web onboarding app at `/onboarding` in addition to REST/Postman.
- The web app includes product/country/applicant filtered mock scenarios, pre-populated demo values, state guidance, policy notes and manual-review notes for interview demonstration.
- The web app renders a generated agreement preview with loan/account terms and requires a click-to-sign checkbox before signing.
- Stores the OpenAPI contract under `src/main/resources/specifications/onboarding-api.yaml` as the intended contract-first source.
- Generates Spring API interfaces/models from the OpenAPI contract during Gradle builds.
- Persists applications, applicants, customers, step answers, IDV checks, decisions, agreements, signing results, resume tokens and audit events with explicit domain IDs and application foreign keys.
- Stores UUID identifiers as readable `char(36)` values in MySQL so demos and support queries show normal UUID strings.
- Enforces step order and server-side required-field validation.
- Uses deterministic local JSON mocks for identity/KYC, address, sanctions/PEP, credit, registry/KYB and bank-account checks.
- Covers agreement creation, sign-now, sign-later and account setup after approval.
- Produces decision outcomes that move applications to agreement, `MANUAL_REVIEW` or `DECLINED`; final success is persisted as `APPROVED` after account setup.
- Supports resumability through opaque expiring resume tokens.
- Includes Temporal workflow types and Docker services for resumable orchestration.

## Prerequisites

- Java 17
- Gradle 9 or the included Gradle wrapper
- Docker Desktop
- AWS CLI
- AWS credentials with access to RDS, EC2 security groups and Secrets Manager for datasource credentials

## Run locally with MySQL + local JSON mocks

```bash
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_SESSION_TOKEN=... # only for temporary credentials
export AWS_REGION=eu-north-1
export IKANOBANK_DB_SECRET_ID=application-db
export IKANOBANK_DB_INSTANCE_ID=ikanobank-onbording-db
export IKANOBANK_DB_NAME=ikanobank-onbording-db

docker compose up -d mysql temporal temporal-ui
./scripts/aws/create-resources.sh
./gradlew bootRun
```

`create-resources.sh` creates an AWS RDS MySQL instance, creates database `ikanobank-onbording-db`, creates app user `admin` with password `admin`, grants permissions, opens MySQL access from your current public IP, and creates/updates Secrets Manager secret `application-db`. Mock integration responses are read from `mock-data/`.

`IKANOBANK_DB_USERNAME` and `IKANOBANK_DB_PASSWORD` are optional provisioning inputs for `create-resources.sh` only. Do not set them in IntelliJ/runtime. The running app reads datasource credentials from Secrets Manager using `IKANOBANK_DB_SECRET_ID`.

Open:

- Web onboarding app: `http://localhost:8080/onboarding`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Temporal UI: `http://localhost:8088`

To run the application container too:

```bash
docker compose --profile app up --build
```

## IntelliJ run configuration

Main class:

```text
com.ikanobank.onboarding.IkanoBankOnboardingApplication
```

Required environment variables:

```text
IKANOBANK_DB_SECRET_ID=application-db
AWS_ACCESS_KEY_ID=<access-key>
AWS_SECRET_ACCESS_KEY=<secret-key>
AWS_REGION=eu-north-1
IKANOBANK_MOCK_DATA_PATH=mock-data
TEMPORAL_ENABLED=false
```

Add `AWS_SESSION_TOKEN` only when using temporary credentials.

Set `TEMPORAL_ENABLED=true` if you want REST calls to start and signal workflows visible in Temporal UI.

Docker Compose pins Temporal to `temporalio/auto-setup:1.29.7` and Temporal UI to `temporalio/ui:2.52.1`.

If `IKANOBANK_DB_SECRET_ID` is set, the app reads datasource values from AWS Secrets Manager. The secret can be a raw password or JSON containing `url`, `username`, and `password`.

Delete the RDS DB and secret when done:

```bash
CONFIRM_DESTROY_DB=$IKANOBANK_DB_INSTANCE_ID \
CONFIRM_DESTROY_DB_SECRET=$IKANOBANK_DB_SECRET_ID \
./scripts/aws/destroy-resources.sh
```

## API demo

Create an application:

```bash
curl -s -X POST http://localhost:8080/api/v1/applications \
  -H 'Content-Type: application/json' \
  -d '{"country":"SWEDEN","customerType":"PRIVATE_INDIVIDUAL","scenarioKey":"1111"}'
```

## Documentation

- [Runbook](docs/RUNBOOK.md): setup, execution, recovery and demo instructions.
- [Architecture](docs/ARCHITECTURE.md): boundaries, data model and orchestration.
- [Assumptions](docs/ASSUMPTIONS.md): explicit tradeoffs.
- [Evaluation coverage](docs/EVALUATION_COVERAGE.md): assignment requirements and rubric mapping.
- [Interview prep](docs/INTERVIEW_PREP.md): likely discussion points.
- [Python reimplementation context](docs/PYTHON_REIMPLEMENTATION_CONTEXT.md): handoff context for a Python version.

The response contains `application.id` and a one-time display of `resumeToken`. Store the token client-side for resume flows.

Submit steps in the order returned by:

```bash
curl http://localhost:8080/api/v1/flows/SWEDEN/PRIVATE_INDIVIDUAL
```

Example identity step:

```bash
curl -X PUT http://localhost:8080/api/v1/applications/APP_ID/steps/identity \
  -H 'Content-Type: application/json' \
  -d '{"answers":{"personalNumber":"19940608-1111"}}'
```

Resume:

```bash
curl -X POST http://localhost:8080/api/v1/applications/resume \
  -H 'Content-Type: application/json' \
  -d '{"resumeToken":"TOKEN_FROM_CREATE_RESPONSE"}'
```

After all steps are complete:

```bash
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/checks
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/submit
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/agreement
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/agreement/sign
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/account-setup
curl http://localhost:8080/api/v1/applications/APP_ID/audit-events
```

For deferred signing:

```bash
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/agreement/sign-later
```

## Mock scenarios

- `identity/1111.json`: pass
- `identity/2222.json`: manual review
- `identity/3333.json`: fail
- `sanctions/4444.json`: manual review
- `sanctions/5555.json`: fail
- `credit/6666.json`: manual review
- `credit/7777.json`: fail
- `agreement/1313.json`: agreement creation fail
- `signing/1414.json`: signing follow-up/manual review
- `account-setup/1515.json`: account setup pending/manual review
- unknown scenario keys fall back to each integration's `default.json`

## Test

```bash
./gradlew test
```

OpenAPI contract validation and source generation are part of the Gradle build. To run them directly:

```bash
./gradlew openApiValidate openApiGenerate
```

CI runs tests and builds the Docker image through `.github/workflows/backend-ci.yml`.
