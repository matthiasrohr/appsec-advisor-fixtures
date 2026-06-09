# appsec-advisor-fixtures

Test fixtures for [`appsec-advisor`](../appsec-advisor). These artifacts live in
a **separate sibling checkout** on purpose: the cross-repo E2E needs scan
targets and an oracle that sit *outside* the plugin repo, so the plugin never
scans or mutates its own test data.

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
oracles/
  cross-repo-threat-fixture/
    verify_threat_model.py    # external oracle — asserts cross-repo signals
    expected-signals.json     # expected report terms, register entries, interfaces
outputs/
  cross-repo-threat-fixture-e2e/   # generated artifacts land here (gitkept, empty)
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

## Running the E2E

The fixtures are driven from the **plugin repo**, not from here:

```bash
cd ../appsec-advisor
./scripts/e2e_cross_repo_fixture.sh --depth quick --clean-output
```

The driver defaults to this sibling checkout
(`../appsec-advisor-fixtures`). Override the location via `--fixture-root`, the
three concrete paths (`--repo` / `--oracle` / `--output`), or the
`APPSEC_CROSS_REPO_E2E_*` environment variables. See
`appsec-advisor/docs/e2e-cross-repo-fixture.md` for the full contract and exit
codes.

> This E2E is **manual and opt-in**. The standard `pytest tests/` run only
> checks the driver/doc contract — it does not run Claude Code or scan these
> fixtures.

## Conventions

- Fixture source is **synthetic and intentionally minimal** — just enough code
  for the boundary to be real. Threats are pre-seeded in the YAML exports, not
  discovered by the scanner.
- Threat-model exports use a far-future `meta.generated` timestamp
  (`2099-01-01`) and stable `commit_sha` values so runs are deterministic.
- `outputs/` is generated; do not commit run artifacts. The driver writes only
  there and never modifies the fixture repos.
