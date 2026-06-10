# AppSec Advisor Spring Boot Threat Fixture

This repository is an intentionally vulnerable Spring Boot application for practical testing of the `appsec-advisor` threat-modeling skill. It is executable, small enough to scan quickly, and contains deliberately planted security signals that should be identified, explained, and rated by a STRIDE-based threat model.

Do not deploy this application outside an isolated local test environment.

## Quick Start

Requirements:

- Java 11
- Maven 3.6+

Run Maven commands from this directory, where `pom.xml` is located. If you are
at the fixture-suite root, either run `cd repos/spring-boot-threat-fixture`
first or pass `-f repos/spring-boot-threat-fixture/pom.xml` to Maven.

Run the tests:

```bash
mvn test
```

Start the application:

```bash
mvn spring-boot:run
```

The app listens on `http://localhost:8080`.

Build and run the executable jar:

```bash
mvn package
java -jar target/spring-boot-threat-fixture-0.1.0.jar
```

## Example Calls

```bash
curl -s http://localhost:8080/actuator/health

curl -s http://localhost:8080/actuator

curl -s http://localhost:8080/actuator/internalSecrets

curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password123"}'

curl -i 'http://localhost:8080/oauth/authorize?response_type=token&client_id=wallet-spa&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Foauth-callback.html&scope=accounts%3Aread%20admin%3Aread&state=demo&login_hint=alice'

curl -s http://localhost:8080/api/profile/me

curl -s 'http://localhost:8080/api/reports/admin-summary?supportOverride=true'

curl -s 'http://localhost:8080/api/accounts/1002?viewer=alice'

curl -s 'http://localhost:8080/api/accounts/search?owner=%27%20OR%20%271%27=%271'

curl -s -X POST http://localhost:8080/api/webhooks/payment-events \
  -H 'Content-Type: application/json' \
  -d '{"accountId":1001,"amountCents":5000,"externalReference":"fixture-webhook"}'
```

## Using It With AppSec Advisor

From the `appsec-advisor` plugin repository, run the fixture E2E against this
external repo:

```bash
./scripts/e2e_spring_fixture.sh --depth quick --clean-output
```

The scan target should be only this repository path. The ground-truth oracle
lives outside the repo at `oracles/spring-boot-threat-fixture`, so the
threat-modeling skill cannot read the expected finding list during normal
repository analysis.

## Fixture Scope

The application models a browser wallet and partner-integration service:

- a Spring Boot web API with password login, OAuth-style browser login, account operations, webhooks, file handling, partner callbacks, and custom actuator operations
- a static SPA under `src/main/resources/static`
- H2-backed runtime data seeded by `DatabaseInitializer`
- Docker, Compose, Nginx, Kubernetes, Terraform/OpenShift, and GitHub Actions deployment metadata
- actuator exposure configured through `management.*` settings and custom actuator code
