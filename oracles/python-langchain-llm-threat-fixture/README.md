# Python LangChain LLM Threat Fixture Oracle

Out-of-repository oracle for `repos/python-langchain-llm-threat-fixture`.

Example:

```bash
python3 oracles/python-langchain-llm-threat-fixture/verify_threat_model.py \
  --repo repos/python-langchain-llm-threat-fixture \
  --report outputs/python-langchain-llm-threat-fixture-e2e/threat-model.md \
  --yaml outputs/python-langchain-llm-threat-fixture-e2e/threat-model.yaml
```

Required signals include LangChain prompt injection exposure, caller-controlled
tools, SSRF, Python code execution, local file reads, missing or weak tenant
authorization, credentialed preview-domain CORS, inline LLM secrets, and
unpinned Python supply-chain configuration.

