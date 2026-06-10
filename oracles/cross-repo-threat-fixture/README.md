# Cross-Repo Threat Fixture Oracle

Out-of-repository oracle for `repos/cross-repo-threat-fixture/consumer-api`.

Example:

```bash
python3 oracles/cross-repo-threat-fixture/verify_threat_model.py \
  --repo repos/cross-repo-threat-fixture/consumer-api \
  --report outputs/cross-repo-threat-fixture-e2e/threat-model.md \
  --yaml outputs/cross-repo-threat-fixture-e2e/threat-model.yaml \
  --output outputs/cross-repo-threat-fixture-e2e
```

Required signals include the report naming both `auth-service` and
`payment-service`, the related-repos loader having consumed
`docs/related-repos.yaml`, and the cross-repo register containing both
producers with their declared interfaces (`POST /internal/auth/verify` and
`POST /internal/payments/charge`).

The oracle also checks that `threat-model.yaml` is a valid YAML mapping.
