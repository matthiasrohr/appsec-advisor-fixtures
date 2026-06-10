# AppSec Advisor Python Threat Fixture

Synthetic FastAPI wallet/API fixture for single-repo threat-model scans. The
code is intentionally vulnerable and should not be deployed outside an isolated
test environment.

## Run

```bash
python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

The app listens on `http://localhost:8000`.

## AppSec Advisor

From the `appsec-advisor` plugin repository:

```bash
./scripts/run-headless.sh \
  --repo ../appsec-advisor-fixtures/repos/python-threat-fixture \
  --output ../appsec-advisor-fixtures/outputs/python-threat-fixture-e2e \
  --full \
  --yaml \
  --sarif \
  --assessment-depth quick
```

Verify the generated report from the fixture-suite root:

```bash
python3 oracles/python-threat-fixture/verify_threat_model.py \
  --repo repos/python-threat-fixture \
  --report outputs/python-threat-fixture-e2e/threat-model.md \
  --yaml outputs/python-threat-fixture-e2e/threat-model.yaml
```

The oracle lives outside the scanned repo.

