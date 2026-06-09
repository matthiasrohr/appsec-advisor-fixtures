# appsec-advisor-fixtures

Test fixtures for [`appsec-advisor`](../appsec-advisor). These artifacts live in
a **separate sibling checkout** on purpose: E2E runs need scan targets and
oracles that sit *outside* the plugin repo, so the plugin never scans or mutates
its own test data.

```text
<workspace>/
  appsec-advisor/            # the plugin under test
  appsec-advisor-fixtures/   # this repo
```

## Layout

```text
repos/
  cross-repo-threat-fixture/
    consumer-api/      # scan target — checkout endpoint calling the producers
    auth-service/      # producer — pre-generated threat-model export only
    payment-service/   # producer — pre-generated threat-model export only
  spring-boot-threat-fixture/  # scan target — Spring Boot OAuth/API fixture
  python-threat-fixture/       # scan target — FastAPI OAuth/API fixture
  rust-threat-fixture/         # scan target — Axum OAuth/API fixture
  go-threat-fixture/           # scan target — net/http OAuth/API fixture
  node-typescript-threat-fixture/  # scan target — Express/TypeScript fixture
  python-langchain-llm-threat-fixture/  # scan target — FastAPI/LangChain fixture
oracles/
  cross-repo-threat-fixture/
    verify_threat_model.py    # external oracle — asserts cross-repo signals
    expected-signals.json     # expected report terms, register entries, interfaces
  spring-boot-threat-fixture/
    verify_threat_model.py    # external oracle — asserts single-repo signals
    expected-signals.json     # expected report evidence groups
  python-threat-fixture/
  rust-threat-fixture/
  go-threat-fixture/
  node-typescript-threat-fixture/
  python-langchain-llm-threat-fixture/
outputs/
  cross-repo-threat-fixture-e2e/   # generated artifacts land here (gitkept, empty)
  spring-boot-threat-fixture-e2e/  # generated artifacts land here (gitkept, empty)
  python-threat-fixture-e2e/
  rust-threat-fixture-e2e/
  go-threat-fixture-e2e/
  node-typescript-threat-fixture-e2e/
  python-langchain-llm-threat-fixture-e2e/
```

## The `cross-repo-threat-fixture`

A consumer service (`consumer-api`) exposes `POST /checkout`, which fans out to
two internal services. Those services are **declared, not scanned**: the
consumer lists them in `consumer-api/docs/related-repos.yaml`, and each producer
ships a pre-generated `docs/security/threat-model.yaml`.

| Repo | Role | Boundary | Seeded threat |
|------|------|----------|---------------|
| `consumer-api` | scan target | `POST /checkout` | — |
| `auth-service` | producer (declared) | `POST /internal/auth/verify` | T-101 — API-key trust instead of JWT issuer/audience validation (CWE-347) |
| `payment-service` | producer (declared) | `POST /internal/payments/charge` | T-201 — unbounded `amount` at the boundary (CWE-20) |

The fixture exercises the plugin's cross-repo context loader, register builder,
STRIDE dispatch slicing, and report output — without the producers being
additional scan targets.

### The oracle

`oracles/cross-repo-threat-fixture/verify_threat_model.py` runs *after* the
pipeline and asserts the expected cross-repo signals defined in
`expected-signals.json`:

- the report mentions `auth-service` and `payment-service`,
- `docs/related-repos.yaml` was loaded (`.related-repos-loaded.json`),
- the cross-repo register (`.cross-repo-register.json`) contains both producers
  with their declared interfaces,
- the rendered `threat-model.yaml` parses as a mapping.

The oracle deliberately lives outside the scanned repo. Exit `0` = all signals
present; non-zero prints `FAIL:` lines per missing signal.

## The `spring-boot-threat-fixture`

A synthetic Spring Boot wallet/API service used for single-repo threat-model
checks. The fixture is intentionally vulnerable and small enough for deterministic
scans.

Seeded design and implementation signals include:

- browser SPA calling APIs directly without a backend-for-frontend boundary,
- OAuth implicit flow returning bearer tokens to URL fragments,
- bearer tokens stored in `localStorage`,
- wildcard credentialed CORS, disabled CSRF, and public `permitAll` API routes,
- public actuator/H2/internal diagnostic endpoints,
- plaintext credentials/tokens, IDOR, SQL injection, NoSQL injection, SSRF,
  unsigned webhooks, file traversal, weak deployment manifests, and missing SCA
  automation.

The source repo is `repos/spring-boot-threat-fixture`. Its oracle lives in
`oracles/spring-boot-threat-fixture` and checks report evidence terms from
`expected-signals.json`; it is not visible to the scanner during normal runs.

## Additional single-repo stack fixtures

The Python, Rust, Go, Node.js TypeScript, and Python/LangChain fixtures mirror
the Spring fixture's browser/API design problems with stack-specific code and
deployment metadata:

| Fixture | Stack | Scan repo | Oracle |
|---------|-------|-----------|--------|
| `python-threat-fixture` | FastAPI/Starlette | `repos/python-threat-fixture` | `oracles/python-threat-fixture` |
| `rust-threat-fixture` | Axum/Tokio | `repos/rust-threat-fixture` | `oracles/rust-threat-fixture` |
| `go-threat-fixture` | Go `net/http` | `repos/go-threat-fixture` | `oracles/go-threat-fixture` |
| `node-typescript-threat-fixture` | Node.js/Express TypeScript | `repos/node-typescript-threat-fixture` | `oracles/node-typescript-threat-fixture` |
| `python-langchain-llm-threat-fixture` | FastAPI/LangChain | `repos/python-langchain-llm-threat-fixture` | `oracles/python-langchain-llm-threat-fixture` |

Each fixture carries the same core architecture signals:

- browser SPA calling APIs directly without a backend-for-frontend boundary,
- OAuth implicit flow returning bearer tokens to URL fragments,
- bearer tokens stored in `localStorage`,
- credentialed preview/partner-origin CORS, missing CSRF controls, and public
  API handlers,
- unverified bearer/JWT-style claim parsing,
- insecure plaintext password handling,
- SQL injection through string-built queries,
- NoSQL injection through raw MongoDB-style filters and `$where`/`$regex`,
- missing authorization plus weak prefix/override-based authorization,
- supply-chain problems such as unpinned dependencies, `npm install` instead
  of deterministic lockfile-based installs, dynamic dependency updates, and
  missing SCA scanning,
- plaintext third-party tokens, IDOR, SSRF, unsigned webhooks, file traversal,
  weak deployment manifests, and missing SCA automation.

The LangChain fixture additionally seeds LLM-specific signals: prompt injection
through retrieved context, caller-controlled tools, SSRF via HTTP fetch tools,
Python code execution, local file reads, prompt/trace secret leakage, and
cross-tenant memory access.

Run them with `scripts/e2e_fixture.sh` from the plugin repo by passing
`--fixture <name>`. The driver scans only `repos/<fixture>`, writes to
`outputs/<fixture>-e2e`, and then runs the matching external oracle.

## Running the E2E

The fixtures are driven from the **plugin repo**, not from here:

```bash
cd ../appsec-advisor
./scripts/e2e_cross_repo_fixture.sh --depth quick --clean-output
./scripts/e2e_fixture.sh --fixture spring-boot-threat-fixture --depth quick --clean-output
./scripts/e2e_fixture.sh --fixture python-langchain-llm-threat-fixture --depth quick --clean-output
```

The drivers default to this sibling checkout (`../appsec-advisor-fixtures`).
Override the location via `--fixture-root`, the concrete paths
(`--repo` / `--oracle` / `--output`), or the fixture-specific environment
variables. See `appsec-advisor/docs/e2e-cross-repo-fixture.md` and
`appsec-advisor/docs/e2e-fixtures.md` for the full contracts and exit codes.
`appsec-advisor/scripts/e2e_spring_fixture.sh` remains available as a
Spring-specific compatibility wrapper.

> This E2E is **manual and opt-in**. The standard `pytest tests/` run only
> checks the driver/doc contract — it does not run Claude Code or scan these
> fixtures.

## Conventions

- Fixture source is **synthetic and intentionally minimal** — just enough code
  for the boundary to be real. Cross-repo producer threats are pre-seeded in
  YAML exports; single-repo threats are inferred from code, configuration, and
  deployment files.
- Scan targets under `repos/` are standalone fixture repos so the plugin's
  `--repo` resolution stays inside the intended scan target.
- Threat-model exports use a far-future `meta.generated` timestamp
  (`2099-01-01`) and stable `commit_sha` values so runs are deterministic.
- `outputs/` is generated; do not commit run artifacts. The driver writes only
  there and never modifies the fixture repos.
