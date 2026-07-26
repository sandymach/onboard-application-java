# Runbook

Supported local setup:

- Spring Boot on your machine
- AWS RDS MySQL for the application database
- MySQL in Docker for Temporal persistence only, not for the application schema
- Temporal in Docker
- AWS Secrets Manager for datasource credentials
- local JSON files under `mock-data/` for mocked integrations

## Required local services

Useful URLs:

- Journey: `http://localhost:8081/onboarding` 

## Provision AWS RDS and Secrets Manager

```bash

export AWS_ACCESS_KEY_ID=
export AWS_REGION=eu-north-1
export AWS_SECRET_ACCESS_KEY=
export IKANOBANK_DB_SECRET_ID=application-db
export IKANOBANK_MOCK_DATA_PATH=mock-data
export TEMPORAL_ENABLED=true;
export TEMPORAL_NAMESPACE=default
export TEMPORAL_TARGET=16.170.108.1:7233
export TEMPORAL_TASK_QUEUE=onboarding
```
## To run, Java 17 and Gradle 8+ are required
./gradlew bootRun
