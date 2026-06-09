# Go Threat Fixture Oracle

Out-of-repository oracle for `repos/go-threat-fixture`.

Example:

```bash
python3 oracles/go-threat-fixture/verify_threat_model.py \
  --repo repos/go-threat-fixture \
  --report outputs/go-threat-fixture-e2e/threat-model.md \
  --yaml outputs/go-threat-fixture-e2e/threat-model.yaml
```

Design signals include OAuth implicit grant, bearer tokens in `localStorage`,
missing BFF, credentialed reflected CORS, absent CSRF/API authorization
boundaries, insecure password handling, string-built SQL, raw MongoDB-style
NoSQL filters, dynamic `go get -u`, and missing `govulncheck`.
