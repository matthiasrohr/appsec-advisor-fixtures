# AGENTS.md

Guidance for AI agents working in `appsec-advisor-fixtures`.

## What this repo is

Test fixtures for the sibling [`appsec-advisor`](../appsec-advisor) plugin. It
holds scan-target repos (as git submodules), producer threat-model exports, and
external oracles. It is **not** an application — there is nothing to build or
deploy here. Read `README.md` first.

The fixture repos under `repos/` are separate GitHub repositories pinned as
submodules. If they appear empty, run `git submodule update --init --recursive`
before doing any work.

## Golden rules

1. **Determinism over realism.** Fixtures are synthetic and minimal. Do not add
   dependencies, real logic, or "more realistic" code unless a test in
   `appsec-advisor` requires it. More code = more drift = flakier E2E.
   Submodule pointers in this repo are pinned commits — do not run
   `git submodule update --remote` without intent; it advances the pin.
2. **The oracle is a contract.** `oracles/cross-repo-threat-fixture/expected-signals.json`
   and `verify_threat_model.py` define what a passing run looks like. If you
   change a fixture's interface, component name, or boundary, update the oracle
   in the same change — and confirm the plugin-side expectations still match.
3. **Producers are declared, not scanned.** `auth-service` and
   `payment-service` exist only to provide `docs/security/threat-model.yaml`
   exports referenced by `consumer-api/docs/related-repos.yaml`. Keep their
   `interface` strings in sync across: the producer YAML, the consumer's
   `related-repos.yaml`, and `expected-signals.json`.
4. **Don't run the E2E from here.** The driver lives in the plugin repo
   (`../appsec-advisor/scripts/e2e_cross_repo_fixture.sh`). Run it from there.
5. **`outputs/` is generated.** Never hand-edit or commit run artifacts; the
   directory is gitkept empty.

## Key invariants (keep these aligned)

For the `cross-repo-threat-fixture`, these four facts must agree everywhere:

| Producer | interface |
|----------|-----------|
| `auth-service` | `POST /internal/auth/verify` |
| `payment-service` | `POST /internal/payments/charge` |

Defined in:
- `repos/cross-repo-threat-fixture/consumer-api/docs/related-repos.yaml`
- each producer's `docs/security/threat-model.yaml` (`attack_surface.entry_point` + `components.paths`)
- `oracles/cross-repo-threat-fixture/expected-signals.json` (`required_register_interfaces`)

The oracle also requires the report to contain the literal terms `auth-service`
and `payment-service` (`required_report_terms`).

## Common tasks

- **First checkout:** after cloning this repo run
  `git submodule update --init --recursive` to populate `repos/`.
- **Update a fixture to a newer commit:** run
  `git submodule update --remote repos/<fixture>`, then commit the updated
  submodule pointer in this repo.
- **Add a producer service:** create the service repo on GitHub, register it as
  a submodule under `repos/cross-repo-threat-fixture/<svc>/`, add a
  `docs/security/threat-model.yaml` export, update the consumer's
  `related-repos.yaml`, and add the name + interface to `expected-signals.json`.
  Then run the plugin-side E2E to confirm.
- **Tighten/loosen what the oracle checks:** edit `expected-signals.json`
  (declarative) before touching `verify_threat_model.py` (logic).
- **Inspect a failed run:** the driver passes `--keep-runtime-files`, so
  `outputs/cross-repo-threat-fixture-e2e/` will contain
  `.related-repos-loaded.json`, `.cross-repo-register.json`, and per-component
  dispatch context.

## Verifying a change

There are no tests in this repo. Validate fixture edits from the plugin:

```bash
cd ../appsec-advisor
pytest tests/test_e2e_cross_repo_fixture_script.py   # driver/doc contract (fast, no scan)
./scripts/e2e_cross_repo_fixture.sh --depth quick --clean-output   # full run (manual, costly)
```

A green oracle (exit `0`, `PASS: expected cross-repo signals present`) is the
success criterion for any fixture change.

## Style

- Match the existing terse, synthetic style. No speculative fields, no comments
  explaining obvious code.
- Keep timestamps far-future (`2099-...`) and `commit_sha` values stable so runs
  stay deterministic.
- Surgical edits only — touch a fixture file only when a specific test or oracle
  signal requires it.
