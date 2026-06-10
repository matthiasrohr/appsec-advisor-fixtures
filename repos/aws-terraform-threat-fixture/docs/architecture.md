# Architecture Notes

This fixture models a small serverless wallet event ingestion platform.

## Actors

- Anonymous internet caller: can invoke the public API Gateway endpoint.
- Payment provider: posts webhook-style events to the Lambda integration.
- Operator: runs Terraform from GitHub Actions.
- AWS control plane: provisions Lambda, IAM, S3, DynamoDB, SQS, EventBridge,
  Bedrock, SSM Parameter Store, CloudWatch Logs, security groups, and RDS
  resources.

## Components

- Terraform root module: owns all AWS infrastructure definitions.
- API Gateway REST API: exposes a public proxy method with no authorizer.
- Lambda function: processes wallet events and reads runtime secrets from
  environment variables.
- IAM role and inline policy: grants broad service actions across all resources.
- S3 upload bucket: accepts public object access.
- S3 public reports bucket: exposes static content through a public website
  endpoint.
- Bedrock agent: invokes a foundation model without a guardrail configuration.
- SSM Parameter Store: stores a partner API token as an unencrypted `String`.
- DynamoDB table: stores billing events without point-in-time recovery.
- SQS queue: receives background jobs without a dead-letter queue.
- EventBridge rule: invokes Lambda on a schedule with privileged input.
- RDS instance: is declared public and unencrypted.
- GitHub Actions workflow: plans and applies Terraform automatically.

## Trust Boundaries

- Internet to API Gateway without an authorizer.
- API Gateway to Lambda execution.
- Lambda execution role to AWS account resources.
- Lambda and Bedrock agent to foundation-model invocation without guardrails.
- Terraform state to plaintext secrets and operator outputs.
- Parameter Store plaintext values to Lambda runtime configuration.
- Public network to RDS and security-group controlled resources.
- CI runner to AWS credentials and Terraform apply.
