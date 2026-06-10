# AppSec Advisor Python LangChain LLM Threat Fixture

Synthetic FastAPI/LangChain LLM integration fixture for single-repo threat-model
scans. The code is intentionally vulnerable and should not be deployed outside
an isolated test environment.

## Run

```bash
python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8010
```

The app listens on `http://localhost:8010`.

## AppSec Advisor

From the `appsec-advisor` plugin repository:

```bash
./scripts/run-headless.sh \
  --repo ../appsec-advisor-fixtures/repos/python-langchain-llm-threat-fixture \
  --output ../appsec-advisor-fixtures/outputs/python-langchain-llm-threat-fixture-e2e \
  --full \
  --yaml \
  --sarif \
  --assessment-depth quick
```

Verify the generated report from the fixture-suite root:

```bash
python3 oracles/python-langchain-llm-threat-fixture/verify_threat_model.py \
  --repo repos/python-langchain-llm-threat-fixture \
  --report outputs/python-langchain-llm-threat-fixture-e2e/threat-model.md \
  --yaml outputs/python-langchain-llm-threat-fixture-e2e/threat-model.yaml
```

