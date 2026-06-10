# Spring Boot Threat Fixture Oracle

This directory is the out-of-repository oracle for
`repos/spring-boot-threat-fixture`.

Keep this directory outside the repository passed to `appsec-advisor --repo`.
The threat-modeling skill should analyze the application code and deployment
files, then this oracle can be run afterward against the generated report.

Example:

```bash
python3 oracles/spring-boot-threat-fixture/verify_threat_model.py \
  --repo repos/spring-boot-threat-fixture \
  --report outputs/spring-boot-threat-fixture-e2e/threat-model.md \
  --yaml outputs/spring-boot-threat-fixture-e2e/threat-model.yaml
```

The verifier is intentionally text based. It does not require exact finding IDs
or exact wording. It checks whether each expected security signal is represented
by enough report evidence terms to support a finding.

Design signals are part of the oracle contract:

- `AUTH-002`: OAuth implicit grant returns bearer tokens to the browser fragment.
- `SPA-001`: the SPA stores bearer tokens in `localStorage` and calls APIs
  directly without a backend-for-frontend boundary.
- `CORS-001`: credentialed preview/partner-origin CORS, disabled CSRF, and
  `permitAll` routes weaken the browser trust boundary.
- `ACTUATOR-001`: public actuator and internal diagnostic endpoints expose
  sensitive operational state.
- `NOSQL-001`: document search accepts raw MongoDB-style filters and `$where`
  clauses.
- `DEPLOY-004`: Terraform-managed OpenShift metadata grants elevated cluster
  access, stores secrets in state, and exposes an insecure route.
- `SUPPLY-001`: CI uses non-deterministic Maven update resolution and lacks
  dependency SCA.
