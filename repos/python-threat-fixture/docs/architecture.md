# Architecture Notes

This fixture models a small Python FastAPI wallet and partner-integration API.

## Actors

- Anonymous internet user: can reach all HTTP endpoints.
- Browser SPA user: receives OAuth access tokens in URL fragments and stores
  them in `localStorage`.
- Account holder: reads and updates account records.
- Payment provider: sends webhook events.
- Partner service: is called through user-supplied callback URLs.
- Operator: uses admin settlement override during support.

## Components

- FastAPI app: exposes `/api/**`, `/oauth/**`, and static browser assets.
- Security middleware: wildcard CORS with credentials and no CSRF boundary.
- OAuth demo endpoint: returns bearer tokens to the browser fragment.
- Static SPA: calls API routes directly without a backend-for-frontend.
- In-memory data store: holds users, accounts, and third-party provider tokens.
- Integration endpoint: fetches user-supplied URLs.
- Document search endpoint: accepts raw MongoDB-style filters and `$where`
  clauses.
- File endpoint: stores and reads caller-controlled paths.

## Trust Boundaries

- Internet to FastAPI API.
- Browser SPA to API without a BFF boundary.
- Browser redirect boundary where OAuth tokens are delivered in URL fragments.
- API to arbitrary user-supplied URLs.
- Container runtime to application process and baked environment variables.
