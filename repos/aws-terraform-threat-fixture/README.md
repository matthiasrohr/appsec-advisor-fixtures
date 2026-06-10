# AppSec Advisor AWS Terraform Threat Fixture

Synthetic Terraform-only AWS infrastructure fixture for single-repo threat-model
scans. It models a small serverless wallet ingestion stack and intentionally
plants insecure infrastructure-as-code signals.

Do not apply this Terraform outside an isolated disposable account.

## Scope

This fixture contains only infrastructure metadata:

- AWS API Gateway routes public requests to Lambda
- Amazon Bedrock is declared without a guardrail configuration
- Lambda uses environment variables for runtime secrets
- IAM roles and inline policies grant broad permissions
- S3, SSM Parameter Store, DynamoDB, SQS, EventBridge, CloudWatch Logs, security
  groups, and RDS are declared from Terraform
- GitHub Actions runs Terraform without IaC security scanning

## AppSec Advisor

From the `appsec-advisor` plugin repository:

```bash
./scripts/e2e_fixture.sh --fixture aws-terraform-threat-fixture --depth quick --clean-output
```

Verify the generated report from the fixture-suite root:

```bash
python3 oracles/aws-terraform-threat-fixture/verify_threat_model.py \
  --repo repos/aws-terraform-threat-fixture \
  --report outputs/aws-terraform-threat-fixture-e2e/threat-model.md \
  --yaml outputs/aws-terraform-threat-fixture-e2e/threat-model.yaml
```
