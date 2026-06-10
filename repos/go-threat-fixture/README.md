# AppSec Advisor Go Threat Fixture

Synthetic Go `net/http` wallet/API fixture for single-repo threat-model scans.
The code is intentionally vulnerable and should not be deployed outside an
isolated test environment.

## Run

```bash
go run ./cmd/server
```

The app listens on `http://localhost:8082`.

## AppSec Advisor

From the `appsec-advisor` plugin repository:

```bash
./scripts/run-headless.sh \
  --repo ../appsec-advisor-fixtures/repos/go-threat-fixture \
  --output ../appsec-advisor-fixtures/outputs/go-threat-fixture-e2e \
  --full \
  --yaml \
  --sarif \
  --assessment-depth quick
```

Verify the generated report from the fixture-suite root:

```bash
python3 oracles/go-threat-fixture/verify_threat_model.py \
  --repo repos/go-threat-fixture \
  --report outputs/go-threat-fixture-e2e/threat-model.md \
  --yaml outputs/go-threat-fixture-e2e/threat-model.yaml
```

