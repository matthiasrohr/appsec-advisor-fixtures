# AWS Terraform Threat Fixture Oracle

Out-of-repository oracle for `repos/aws-terraform-threat-fixture`.

Example:

```bash
python3 oracles/aws-terraform-threat-fixture/verify_threat_model.py \
  --repo repos/aws-terraform-threat-fixture \
  --report outputs/aws-terraform-threat-fixture-e2e/threat-model.md \
  --yaml outputs/aws-terraform-threat-fixture-e2e/threat-model.yaml
```

Expected signals include unauthenticated API Gateway to Lambda, Bedrock without
guardrails, wildcard IAM, plaintext secrets in Terraform state, CI, and SSM
Parameter Store, public S3 object access, weak RDS, DynamoDB, and SQS data
controls, wide-open networking, missing access logging, and Terraform auto-apply
without IaC security scanning.
