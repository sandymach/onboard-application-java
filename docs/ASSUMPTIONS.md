# Assumptions and Tradeoffs

- The implementation remains Java 17/Spring Boot even though the brief mentions Python. This is deliberate for this repo phase.
- The assignment asks for a web app. This repo provides a server-rendered web journey at `/onboarding`, plus REST APIs and OpenAPI documentation.
- Country-specific rules are representative mocks, not legal compliance statements.
- No real KYC, registry, credit bureau, eID or bank APIs are called.
- Mock integrations use deterministic local JSON files under `mock-data/`.
- Local development uses AWS Secrets Manager for datasource credentials. Temporary credentials are preferred; long-lived access keys work without `AWS_SESSION_TOKEN`.
- Temporal is introduced as the orchestration foundation, while business rules stay outside workflows for readability and testability.
- MySQL with Flyway is the supported local database path.
