# Architecture Notes

This fixture models a small Rust Axum wallet and partner-integration API.

## Actors

- Anonymous internet user: can reach all HTTP endpoints.
- Browser SPA user: receives OAuth access tokens in URL fragments and stores
  them in `localStorage`.
- Account holder: reads account records.
- Payment provider: sends webhook events.
- Partner service: is called through user-supplied URLs.

## Components

- Axum API: exposes `/api/**`, `/oauth/**`, and static browser assets.
- CORS layer: wildcard origin, wildcard headers, credentials, and no CSRF
  boundary.
- OAuth demo endpoint: returns bearer tokens to the browser fragment.
- Static SPA: calls API routes directly without a backend-for-frontend.
- In-memory accounts and provider-token maps.
- Integration endpoint: fetches user-supplied URLs with `reqwest`.
- Document search endpoint: accepts raw MongoDB-style filters and `$where`
  clauses.
- File endpoint: reads caller-controlled paths with `tokio::fs`.

## Trust Boundaries

- Internet to Axum API.
- Browser SPA to API without a BFF boundary.
- Browser redirect boundary where OAuth tokens are delivered in URL fragments.
- API to arbitrary user-supplied URLs.
- Container runtime to application process and baked environment variables.
