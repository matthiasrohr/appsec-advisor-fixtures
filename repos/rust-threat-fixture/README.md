# AppSec Advisor Rust Threat Fixture

Synthetic Axum/Tokio wallet/API fixture for single-repo threat-model scans. The
code is intentionally vulnerable and should not be deployed outside an isolated
test environment.

## Run

```bash
cargo run
```

The app listens on `http://localhost:8081`.

## AppSec Advisor

From the `appsec-advisor` plugin repository:

```bash
./scripts/run-headless.sh \
  --repo ../appsec-advisor-fixtures/repos/rust-threat-fixture \
  --output ../appsec-advisor-fixtures/outputs/rust-threat-fixture-e2e \
  --full \
  --yaml \
  --sarif \
  --assessment-depth quick
```

Verify the generated report from the fixture-suite root:

```bash
python3 oracles/rust-threat-fixture/verify_threat_model.py \
  --repo repos/rust-threat-fixture \
  --report outputs/rust-threat-fixture-e2e/threat-model.md \
  --yaml outputs/rust-threat-fixture-e2e/threat-model.yaml
```

