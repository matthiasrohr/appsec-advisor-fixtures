# Node TypeScript Threat Fixture Oracle

Out-of-repository oracle for `repos/node-typescript-threat-fixture`.

Example:

```bash
python3 oracles/node-typescript-threat-fixture/verify_threat_model.py \
  --repo repos/node-typescript-threat-fixture \
  --report outputs/node-typescript-threat-fixture-e2e/threat-model.md \
  --yaml outputs/node-typescript-threat-fixture-e2e/threat-model.yaml
```

Required signals include OAuth implicit grant, insecure password handling,
string-built SQL, raw MongoDB-style NoSQL filters, bearer tokens in
`localStorage`, missing BFF, credentialed dynamic CORS, missing CSRF/API
authorization boundaries, dependency ranges, `npm install` in CI, no lockfile,
and missing `npm audit`.
