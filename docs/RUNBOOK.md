# Runbook

Supported local setup:

- Spring Boot on your machine or in Docker
- AWS RDS MySQL for the application database
- MySQL in Docker for Temporal persistence only, not for the application schema
- Temporal in Docker
- AWS Secrets Manager for datasource credentials
- local JSON files under `mock-data/` for mocked integrations

Do not paste AWS keys into chat, source code, commits, screenshots, or Postman collections. Put them only in your shell, IntelliJ Run Configuration, or a local untracked `.env` file.

## Required local services

```bash
docker compose up -d mysql temporal temporal-ui
```

Useful URLs:

- API docs: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Temporal UI: `http://localhost:8088`

Temporal Docker images are pinned to stable current tags:

- `temporalio/auto-setup:1.29.7`
- `temporalio/ui:2.52.1`

## Required AWS permissions

Use an IAM user or role with the smallest required permissions. Avoid AWS root credentials for development.

Minimum AWS permissions:

- `sts:GetCallerIdentity`
- `ec2:DescribeVpcs`
- `ec2:DescribeSecurityGroups`
- `ec2:CreateSecurityGroup`
- `ec2:AuthorizeSecurityGroupIngress`
- `ec2:DeleteSecurityGroup`
- `rds:CreateDBInstance`
- `rds:DescribeDBInstances`
- `rds:ModifyDBInstance`
- `rds:DeleteDBInstance`
- `secretsmanager:CreateSecret`
- `secretsmanager:DescribeSecret`
- `secretsmanager:PutSecretValue`
- `secretsmanager:GetSecretValue`
- `secretsmanager:DeleteSecret`

If you use temporary credentials, include `AWS_SESSION_TOKEN`:

```bash
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_SESSION_TOKEN=...
export AWS_REGION=eu-north-1
```

If you use long-lived access keys, omit `AWS_SESSION_TOKEN`:

```bash
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_REGION=eu-north-1
```

## Provision AWS RDS and Secrets Manager

`create-resources.sh` creates an AWS RDS MySQL database and creates/updates the AWS Secrets Manager datasource secret.

```bash
docker compose up -d mysql temporal temporal-ui

export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_REGION=eu-north-1
export IKANOBANK_DB_SECRET_ID=application-db
export IKANOBANK_DB_INSTANCE_ID=ikanobank-onbording-db
export IKANOBANK_DB_NAME=ikanobank-onbording-db
export IKANOBANK_DB_USERNAME=admin
export IKANOBANK_DB_PASSWORD=admin

./scripts/aws/create-resources.sh
```

`IKANOBANK_DB_USERNAME` and `IKANOBANK_DB_PASSWORD` are provisioning inputs only. They are written into Secrets Manager. Do not set them in IntelliJ or normal app runtime.

The application DB user/password defaults are `admin/admin`. RDS itself uses an internal generated master password because AWS RDS does not allow `admin` as a valid master password length.

To override the app DB password:

```bash
export IKANOBANK_DB_PASSWORD=...
```

The secret value is JSON:

```json
{
  "url": "jdbc:mysql://<rds-endpoint>:3306/ikanobank-onbording-db?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC",
  "username": "admin",
  "password": "admin"
}
```

The script creates a security group ingress rule for your current public IP on port `3306`. If your IP changes, rerun the script.

## Mock data

Mock integration responses are loaded from local JSON files:

```text
mock-data/
  identity/
  address/
  sanctions/
  credit/
  registry/
  bank-account/
  agreement/
  signing/
  account-setup/
```

Default path:

```text
mock-data
```

Override path when needed:

```bash
export IKANOBANK_MOCK_DATA_PATH=/absolute/path/to/mock-data
```

## Status model

The database stores lifecycle states, not only decision states:

- `CREATED`, `INITIATED`, `IN_PROGRESS`
- `KYC`, `IDV`
- `READY_FOR_REVIEW`
- `MANUAL_REVIEW`
- `DECLINED`, `CANCELLED`
- `AGREEMENT_CREATED`, `SIGNING_PENDING`, `AGREEMENT_SIGNED`
- `APPROVED`

`APPROVED` is the final successful state after account setup. A positive decision after checks moves the journey to `AGREEMENT_CREATED`.

## Run Spring Boot from terminal

```bash
export IKANOBANK_DB_SECRET_ID=application-db
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_REGION=eu-north-1
export IKANOBANK_MOCK_DATA_PATH=mock-data

./gradlew bootRun
```

Enable the Temporal worker as well:

```bash
TEMPORAL_ENABLED=true ./gradlew bootRun
```

## Run Spring Boot from IntelliJ

Create a Spring Boot run configuration:

- Main class: `com.ikanobank.onboarding.IkanoBankOnboardingApplication`
- Module/classpath: `ikanobank-onboarding-backend.main`
- JDK: Java 17
- Working directory: project root
- Active profiles: leave empty

Final IntelliJ environment variables for long-lived AWS keys:

```text
IKANOBANK_DB_SECRET_ID=application-db
AWS_ACCESS_KEY_ID=<your-access-key>
AWS_SECRET_ACCESS_KEY=<your-secret-key>
AWS_REGION=eu-north-1
IKANOBANK_MOCK_DATA_PATH=mock-data
TEMPORAL_ENABLED=false
TEMPORAL_TARGET=localhost:7233
TEMPORAL_NAMESPACE=default
TEMPORAL_TASK_QUEUE=onboarding
```

Add `AWS_SESSION_TOKEN` only when using temporary AWS credentials.

Set `TEMPORAL_ENABLED=true` in IntelliJ when you want create/step/check REST calls to show workflows in Temporal UI.

Do not set:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

The app reads datasource values from AWS Secrets Manager when `IKANOBANK_DB_SECRET_ID` is present.

## Run the application container

The application container expects AWS credentials from your shell or `.env`:

```bash
./scripts/aws/create-resources.sh
docker compose --profile app up --build
```

## Flyway/MySQL recovery

The application schema now stores UUID IDs as readable `char(36)` strings. If your existing dev DB was created before this change, Workbench may show binary-looking characters in ID columns. For the demo, reset the disposable application schema once so Flyway recreates the tables with readable IDs:

```bash
mysql -h "$DB_HOST" -P 3306 -uadmin -padmin ikanobank-onbording-db < scripts/db/reset-application-schema.sql
```

Then restart Spring Boot. New records will show UUIDs like `dfad7efc-3cbc-4158-9ce9-13d66f0b1257`.

The AWS dev schema was reset and recreated through Spring Boot/Flyway on 2026-07-23. Verified ID columns:

- `application.application_id` and technical records: readable UUID `char(36)`.
- `applicant.applicant_id`: numeric `char(10)`.
- `customer.customer_id`: numeric `char(10)`.
- `idv.applicant_id`: numeric `char(10)`.

Applicant/customer IDs are generated as 10 numeric characters from the application UUID. Applicant IDs start with `1`; customer IDs start with `2`.

## Error response scenarios

The OpenAPI contract documents standard non-sensitive error responses:

- `400 Bad Request`: malformed JSON, invalid enum/path value, missing required fields, failed regex validation, failed age/salary/debt validation.
- `403 Forbidden`: request understood but forbidden for caller/context. The demo has no auth layer, but the global handler maps security exceptions to this contract.
- `404 Not Found`: application or flow does not exist.
- `409 Conflict`: operation is valid but the application is in the wrong lifecycle state, for example creating agreement before approval.
- `500 Internal Server Error`: unexpected server error with safe generic message.

Error responses include timestamp, HTTP status, reason phrase, safe message and request ID.

The web app also includes an “Error scenario demos” panel:

- `400 validation failure`
- `404 unknown application`
- `409 wrong lifecycle state`

Use those buttons during the interview if you want to show validation/error behavior from the browser. `403` and `500` are documented in OpenAPI/global handling; the demo avoids adding fake auth/crash endpoints only to force those responses.

If local data does not matter:

```bash
docker compose down -v
docker compose up -d mysql temporal temporal-ui
./scripts/aws/create-resources.sh
./gradlew bootRun
```

If Flyway reports `Validate failed: Migrations have failed validation` and you must preserve local data:

```bash
./gradlew flywayRepair
./gradlew bootRun
```

If the failed migration is in the AWS/local application database used by IntelliJ, repair only failed Flyway rows:

```bash
mysql -h "$DB_HOST" -P 3306 -uadmin -padmin ikanobank-onbording-db < scripts/db/repair-application-flyway.sql
```

Then restart the Spring Boot app.

If the schema is disposable and you want a clean local/dev rebuild:

```bash
mysql -h "$DB_HOST" -P 3306 -uadmin -padmin ikanobank-onbording-db < scripts/db/reset-application-schema.sql
```

Then restart the Spring Boot app. Flyway will recreate the application schema from `V1` onward.

## Test and validation

```bash
./gradlew test
./gradlew openApiValidate openApiGenerate
bash -n scripts/aws/create-resources.sh scripts/aws/destroy-resources.sh
docker compose config --quiet
docker build -t ikanobank-onboarding-backend:test .
```

Generated OpenAPI sources are written under `build/generated/openapi/src/main/java` and are not hand-edited.

## Postman scenarios

The Postman collection is stored in:

- `docs/postman/Ikano_Onboarding.postman_collection.json`
- `docs/postman/Ikano_Onboarding.scenarios.postman_data.json`

Runner setup:

1. Start services:

   ```bash
   docker compose up -d mysql temporal temporal-ui
   ```

2. Provision DB/secret:

   ```bash
   ./scripts/aws/create-resources.sh
   ```

3. Start the app. Use `TEMPORAL_ENABLED=true` when you want workflows to appear in Temporal UI:

   ```bash
   TEMPORAL_ENABLED=true ./gradlew bootRun
   ```

4. Import `docs/postman/Ikano_Onboarding.postman_collection.json`.
5. Open Collection Runner.
6. Select the whole collection. It now contains only runner-safe requests.
7. Load `docs/postman/Ikano_Onboarding.scenarios.postman_data.json` as the data file.
8. Run all iterations.

The scenario data covers all six assignment flows and deterministic approved/manual-review/rejected outcomes.

Approved scenarios continue through agreement creation, signing and account setup. Manual-review/rejected scenarios stop after final decision.

The main collection intentionally excludes standalone utility requests such as health checks and sign-later. This prevents Collection Runner from executing sign-later after account setup, which correctly returns `400`.

Every Postman request sends `X-Transaction-Id`, `X-Trace-Id`, `X-Product-Code` and `X-Channel` headers. These values are emitted in application logs.

## Temporal UI workflow execution

Temporal UI is available at `http://localhost:8088`. The backend registers the workflow worker only when `TEMPORAL_ENABLED=true`.

The current Temporal structure is:

- product workflow: `OnboardingWorkflow`
- reusable child workflow: `IdvJourneyWorkflow`
- reusable child workflow: `AgreementSigningWorkflow`

The REST path starts and signals the product workflow. The product workflow then starts reusable Temporal child workflows for IDV and agreement/signing instead of duplicating country-specific orchestration logic.

Temporal UI is not a replacement for the web app or Postman runner. It can start registered workflows, but the complete onboarding scenario needs REST calls because application creation, validation, database writes, mock checks, decisions, agreements and manual override live in the Spring service/API layer. To run full mock scenarios from Temporal UI, add a dedicated admin-only scenario-runner workflow with activities that call the same service operations. That is feasible, but not enabled by default because it bypasses the customer-facing entrypoints and should not exist in production without access control.

Expected workflow completion behavior:

- approved scenario: product workflow remains running until account setup completes, then completes.
- declined scenario: product workflow completes after final decision.
- manual-review/referred scenario: product workflow remains running until manual override.
- manual override to `AGREEMENT_CREATED`: workflow continues through agreement/signing/account setup.
- manual override to `DECLINED` or `CANCELLED`: workflow completes immediately.
- sign-later scenario: workflow remains running in `SIGNING_PENDING` until the customer signs and account setup completes.

Start the backend with the Temporal worker:

```bash
TEMPORAL_ENABLED=true ./gradlew bootRun
```

Run Postman or call `POST /api/v1/applications`. The app starts a workflow automatically with ID:

```text
onboarding-{applicationId}
```

REST step submissions signal `stepCompleted`. Running checks signals `checksRequested`. You should then see workflows in Temporal UI under namespace `default`.

To run a full workflow-backed mock scenario:

1. Start dependencies:

   ```bash
   docker compose up -d mysql temporal temporal-ui
   ```

2. Start the app:

   ```bash
   TEMPORAL_ENABLED=true ./gradlew bootRun
   ```

3. Run the Postman Collection Runner from `Run Scenario - Create Application`.
4. Open `http://localhost:8088`.
5. Search for workflow ID prefix `onboarding-`.

## API smoke test

Create an application:

```bash
curl -s -X POST http://localhost:8080/api/v1/applications \
  -H 'Content-Type: application/json' \
  -d '{"country":"SWEDEN","customerType":"PRIVATE_INDIVIDUAL","scenarioKey":"1111"}'
```

Submit steps in the order returned by:

```bash
curl http://localhost:8080/api/v1/flows/SWEDEN/PRIVATE_INDIVIDUAL
```

Run checks and submit:

```bash
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/checks
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/submit
curl http://localhost:8080/api/v1/applications/APP_ID/audit-events
```

After an approved final decision, continue the onboarding journey:

```bash
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/agreement
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/agreement/sign
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/account-setup
```

For sign-later:

```bash
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/agreement
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/agreement/sign-later
```

## Manual override for referred applications

Applications in `MANUAL_REVIEW` can be manually overridden. This is the manual review execution path:

1. Use a scenario that returns manual review, then complete steps, run checks and submit.
2. Confirm the application status is `MANUAL_REVIEW`.
3. Apply manual override to either continue the approved journey or decline/cancel the application.

Approve after manual review:

```bash
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/manual-override \
  -H 'Content-Type: application/json' \
  -d '{"status":"AGREEMENT_CREATED","reason":"manual approval after document review"}'
```

After this, continue with:

```bash
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/agreement
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/agreement/sign
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/account-setup
```

Decline after manual review:

```bash
curl -X POST http://localhost:8080/api/v1/applications/APP_ID/manual-override \
  -H 'Content-Type: application/json' \
  -d '{"status":"DECLINED","reason":"manual decline after document review"}'
```

Allowed override statuses:

- `AGREEMENT_CREATED`
- `DECLINED`
- `CANCELLED`

The override creates a `decision` row with `decision_source=MANUAL_OVERRIDE`, records an `application_events` row and updates the `application.status`.

## CloudWatch observability

The app writes structured logs to stdout using `logback.xml`. In AWS, publish these logs to CloudWatch using the runtime log driver, for example ECS/Fargate `awslogs`:

```json
{
  "logDriver": "awslogs",
  "options": {
    "awslogs-group": "/ikano/onboarding/backend",
    "awslogs-region": "eu-north-1",
    "awslogs-stream-prefix": "app"
  }
}
```

Each log line includes:

- `traceId`
- `transactionId`
- `requestId`
- `applicationId`
- `productCode`
- `country`
- `customerType`
- `channel`

MDC is intentionally limited to non-sensitive observability fields. It does not include resume tokens, AWS secrets, DB credentials, raw step answers, personal numbers, document numbers, request bodies or integration payloads. Header-derived MDC values are sanitized and length-limited before logging.

## Contract-first API

The API contract is stored under:

```text
src/main/resources/specifications/onboarding-api.yaml
```

Gradle validates this contract and generates Spring API interfaces/models. `OnboardingController` implements the generated interfaces and maps generated public models to internal service DTOs. Keep all future public API contracts under `src/main/resources/specifications`.

## Web onboarding app

The assignment asks for a web onboarding app. A minimal server-rendered entrypoint is available at:

```text
http://localhost:8080/onboarding
```

It demonstrates:

- Ikano Bank onboarding branding throughout the customer journey;
- product-code selector (`IKANO_ONBOARDING_LOAN`, `IKANO_BUSINESS_ACCOUNT`);
- country selector;
- account-type selector;
- a first setup screen for scenario presets with notes for approved, manual-review and rejected paths;
- filtered mock-scenario selector based on product code, country and applicant type;
- detailed mock-data explanation on the first screen, including expected outcome, selected mock key and policy/check notes;
- back-to-scenarios and start-new-application navigation controls;
- scenario selector;
- adaptive step progression using the selected flow;
- pre-populated mock field values for demo speed;
- next-action guidance and disabled buttons for actions that are not valid in the current state;
- underwriting/AML/fraud/KYC/KYB policy notes for approve, manual-review and decline outcomes;
- validation feedback through the backend API;
- mocked checks;
- decision submission;
- manual approve/decline;
- agreement/signing/account setup with sample PDF documents;
- audit/state output.

This is intentionally lightweight and uses the existing REST API. It is enough to demonstrate the web journey without adding a separate frontend build toolchain.

Recommended demo scenarios:

- `Happy path - Sweden private`: complete all steps, run checks, submit, agreement, sign, account setup.
- `Manual review - identity`: submit to `MANUAL_REVIEW`, explain referred applications, then use Manual approve or Manual decline.
- `Rejected - identity`: submit to `DECLINED`, show terminal rejection.
- `Business manual review - registry`: show KYB/manual-review behavior for a business flow.

Policy/reason notes shown in the web UI:

- `credit` -> Underwriting
- `sanctions` / `pep` -> AML
- `identity` / `bank-account` -> Fraud/KYC
- `registry` -> KYB
- `address` -> Address verification

When a scenario is referred or declined, the UI shows the policy area, reason code and mock integration message so the decision can be explained during the interview.

After agreement creation, the web page renders an agreement preview with typical loan/account details:

- agreement reference;
- applicant;
- country;
- product;
- credit amount;
- term;
- representative APR;
- fee note;
- legal/demo disclaimer.

The customer must tick the click-to-sign confirmation checkbox before the `Sign agreement` button is enabled.

The sample agreement PDF is also available after agreement creation:

- `/documents/sample-agreement.pdf`

The web page disables the agreement creation button after the document is generated so the demo does not create the same agreement multiple times.

## Requirement coverage from the PDF

- Web onboarding app: covered by `/onboarding`.
- Six country/customer flows: covered by flow definitions and Postman scenario data.
- Server-side validation: covered by `StepAnswerPolicy`.
- Deterministic mocks: covered by local JSON integration clients.
- Pass/manual/fail outcomes: covered by mock scenarios and decisioning.
- Audit trail: covered by `/audit-events`.
- Resumability: covered by resume token flow with expiry.
- Production mindset: request/transaction IDs, structured logs, Secrets Manager, RDS, no raw answer logging.
- Extensibility: product workflow plus reusable IDV and agreement/signing workflow types.

## Application database schema

The application database now uses explicit domain tables instead of one generic application/check/audit shape:

- `application`: aggregate root. Primary key is `application_id`.
- `application_events`: immutable event/audit history. Primary key is `application_event_id`; foreign key `application_id -> application.application_id`.
- `applicant`: applicant-level onboarding identity/person/business participant snapshot. Primary key is `applicant_id`; foreign key `application_id -> application.application_id`.
- `customer`: customer projection for the onboarding journey. Primary key is `customer_id`; foreign key `application_id -> application.application_id`. Status starts as `NEW_CUSTOMER` and becomes `EXISTING_CUSTOMER` after account setup.
- `idv`: identity, sanctions and PEP check outcomes. Primary key is `idv_id`; foreign keys `application_id -> application.application_id` and `applicant_id -> applicant.applicant_id`.
- `decision`: system and manual decision history. Primary key is `decision_id`; foreign key `application_id -> application.application_id`.
- `agreement`: agreement creation result/reference. Primary key is `agreement_id`; foreign key `application_id -> application.application_id`.
- `signing`: sign-now/sign-later result/reference. Primary key is `signing_id`; foreign key `application_id -> application.application_id`.
- `step_result`: raw submitted step answers stored as JSON plus fingerprint. Primary key is `step_result_id`; foreign key `application_id -> application.application_id`.
- `integration_result`: raw deterministic mock integration responses retained for troubleshooting. Primary key is `integration_result_id`; foreign key `application_id -> application.application_id`.

This keeps the assignment API simple while making the schema closer to a production onboarding domain model.

Flyway migrations:

- `V1__initial_schema.sql`: creates diagnostic `step_result` and `integration_result` tables used by the current schema.
- `V3__production_domain_schema.sql`: creates the domain tables for fresh databases.
- `V4__explicit_domain_ids_and_foreign_keys.sql`: converts earlier generic `id` columns to explicit domain IDs and adds missing application foreign keys.

Legacy tables `onboarding_application` and `audit_event` are no longer created. The reset/cleanup scripts still remove them if they exist from an older local schema.

## Cleanup

Delete the AWS RDS database and DB secret:

```bash
CONFIRM_DESTROY_DB=$IKANOBANK_DB_INSTANCE_ID \
CONFIRM_DESTROY_DB_SECRET=$IKANOBANK_DB_SECRET_ID \
./scripts/aws/destroy-resources.sh
```

Clean application data without destroying AWS resources:

```bash
mysql -h "$DB_HOST" -P 3306 -uadmin -padmin ikanobank-onbording-db < scripts/db/cleanup-application-db.sql
```

`cleanup-application-db.sql` truncates onboarding data but keeps the schema and Flyway history. If Flyway itself is failed, use `repair-application-flyway.sql` or `reset-application-schema.sql` instead.

Clean local Temporal workflow execution data:

```bash
docker compose stop app temporal temporal-ui
mysql -h 127.0.0.1 -P 3306 -uroot -pikano_root < scripts/db/cleanup-temporal-db.sql
docker compose up -d temporal temporal-ui
```

Use the Temporal cleanup only for local/dev reset. Do not run it against a shared or production Temporal cluster.

If workflow replay errors continue after cleanup, remove the local MySQL container volume and let Temporal recreate its schemas:

```bash
docker compose down -v
docker compose up -d mysql temporal temporal-ui
```

## GitHub Actions CI/CD

CI is configured in `.github/workflows/backend-ci.yml`:

- checkout;
- Java 17 setup;
- Gradle test;
- Docker image build.

CD skeleton is configured in `.github/workflows/backend-cd.yml`:

- manual or `main`/tag trigger;
- Gradle tests;
- AWS OIDC role assumption;
- Docker image publish to ECR;
- placeholder deployment section for ECS or a remote host.

The CD workflow is guarded by repository variable:

```text
ENABLE_AWS_CD=true
```

Required repository secret:

```text
AWS_DEPLOY_ROLE_ARN=<iam-role-arn-for-github-oidc-deploy>
```

## Contract-first implementation

The OpenAPI contract lives at:

```text
src/main/resources/specifications/onboarding-api.yaml
```

Gradle validates the contract and generates Spring API interfaces/models into:

```text
build/generated/openapi/src/main/java
```

`OnboardingController` implements the generated interfaces:

- `ApplicationsApi`
- `FlowsApi`
- `DecisioningApi`
- `FulfilmentApi`
- `AuditApi`

The controller maps generated public models to the internal service DTOs. Do not edit generated source directly; update `onboarding-api.yaml` and rerun:

```bash
./gradlew openApiGenerate
```

Recommended approach:

1. Keep CI enabled for every PR/push.
2. Keep CD disabled until a real remote target exists.
3. Use GitHub OIDC to assume an AWS IAM role; do not store long-lived AWS keys in GitHub secrets.
4. Deploy to a small AWS target only if cost is acceptable, for example ECS/Fargate, Elastic Beanstalk, EC2, or App Runner.

Cost note as of 2026-07-23:

- GitHub Actions is free for public repositories using standard GitHub-hosted runners.
- Private repositories get included minutes/storage based on the GitHub plan. GitHub Free lists 2,000 Actions minutes/month.
- GitHub Actions may be free, but the remote environment is not automatically free. AWS RDS, ECS/Fargate, App Runner, EC2, NAT gateways, logs and data transfer can cost money.

Then restart the Spring Boot application.

For repeated `ReplayWorkflowTaskHandler` / `InternalWorkflowTaskException` during local development:

1. Stop the Spring Boot app or IntelliJ run configuration.
2. Stop Temporal services.
3. Run `scripts/db/cleanup-temporal-db.sql`.
4. Start Temporal again.
5. Start Spring Boot again.

Do not keep the worker running while deleting Temporal rows; the worker can immediately pick stale tasks while cleanup is in progress.
