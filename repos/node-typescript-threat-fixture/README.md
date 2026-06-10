# AppSec Advisor Node TypeScript Threat Fixture

Synthetic Node.js/TypeScript wallet/API fixture for single-repo threat-model
scans. The code is intentionally vulnerable and should not be deployed outside
an isolated test environment.

## Run

```bash
npm install
npm run dev
```

The app listens on `http://localhost:3000`.

## AppSec Advisor

From the `appsec-advisor` plugin repository:

```bash
./scripts/run-headless.sh \
  --repo ../appsec-advisor-fixtures/repos/node-typescript-threat-fixture \
  --output ../appsec-advisor-fixtures/outputs/node-typescript-threat-fixture-e2e \
  --full \
  --yaml \
  --sarif \
  --assessment-depth quick
```

Verify the generated report from the fixture-suite root:

```bash
python3 oracles/node-typescript-threat-fixture/verify_threat_model.py \
  --repo repos/node-typescript-threat-fixture \
  --report outputs/node-typescript-threat-fixture-e2e/threat-model.md \
  --yaml outputs/node-typescript-threat-fixture-e2e/threat-model.yaml
```

