# Architecture Notes

This fixture models a small online wallet and partner-integration service.

## Actors

- Anonymous internet user: can reach all HTTP endpoints in the fixture.
- Account holder: intended user of account lookup, file upload, and token storage.
- Browser SPA user: receives OAuth access tokens in URL fragments and stores them in `localStorage`.
- OAuth demo provider: local authorization endpoint that grants bearer tokens to the SPA.
- Payment provider: sends webhook events for wallet top-ups.
- Partner service: receives callbacks or has public pages fetched by the integration API.
- Operator: uses admin settlement override and actuator endpoints during local support.

## Components

- Spring Boot web API: exposes `/api/**`, `/actuator/**`, and `/h2-console/**`.
- Security configuration: central request policy and CORS handling for browser and API clients.
- H2 database: stores users, account balances, third-party tokens, and webhook event history.
- Auth controller: authenticates with username/password and issues demo JWT-like tokens.
- OAuth controller: browser authorization endpoint and userinfo endpoint for the SPA.
- Static SPA: calls API routes directly from the browser.
- Account controller: reads and updates account records.
- Document search route: accepts raw MongoDB-style filters and `$where` clauses.
- Protected-resource controller: exposes profile and reporting APIs used by the SPA.
- Admin controller: changes balances when a static header token is supplied.
- Webhook controller: accepts payment events from a supposed payment provider.
- Integration controller: fetches user-supplied URLs and posts callbacks to user-supplied URLs.
- File controller: stores uploaded files and downloads local files by request parameter.
- Actuator endpoints: built-in Spring Boot endpoints plus a custom internal-state endpoint for local diagnostics.
- CI and deployment metadata: GitHub Actions, Docker, Compose, Nginx, Kubernetes, and Terraform-managed OpenShift files.

## Trust Boundaries

- Internet to Spring Boot API.
- Browser SPA to Spring Boot API without a backend-for-frontend boundary.
- Browser redirect boundary where OAuth tokens are delivered in URL fragments.
- Spring Boot API to H2 database.
- Spring Boot API to arbitrary URLs supplied through integration endpoints.
- GitHub Actions runner to dependency repositories and build artifacts.
- Container runtime to application process and baked environment variables.
- Terraform operator to the OpenShift API and cluster-managed route.
