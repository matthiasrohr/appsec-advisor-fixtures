# Python Threat Fixture Oracle

Out-of-repository oracle for `repos/python-threat-fixture`.

Example:

```bash
python3 oracles/python-threat-fixture/verify_threat_model.py \
  --repo repos/python-threat-fixture \
  --report outputs/python-threat-fixture-e2e/threat-model.md \
  --yaml outputs/python-threat-fixture-e2e/threat-model.yaml
```

Design signals include OAuth implicit grant, bearer tokens in `localStorage`,
missing BFF, credentialed preview-domain CORS, absent CSRF/API authorization
boundaries, insecure password handling, string-built SQL, raw MongoDB-style
NoSQL filters, unpinned Python dependencies, and pip installs without hashes.
