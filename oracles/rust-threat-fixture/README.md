# Rust Threat Fixture Oracle

Out-of-repository oracle for `repos/rust-threat-fixture`.

Example:

```bash
python3 oracles/rust-threat-fixture/verify_threat_model.py \
  --repo repos/rust-threat-fixture \
  --report outputs/rust-threat-fixture-e2e/threat-model.md \
  --yaml outputs/rust-threat-fixture-e2e/threat-model.yaml
```

Design signals include OAuth implicit grant, bearer tokens in `localStorage`,
missing BFF, credentialed preview-domain CORS, absent CSRF/API authorization
boundaries, insecure password handling, string-built SQL, raw MongoDB-style
NoSQL filters, dynamic `cargo update`, and missing `cargo audit`.
