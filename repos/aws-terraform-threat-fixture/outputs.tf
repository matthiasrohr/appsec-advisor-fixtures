output "public_api_execution_arn" {
  value = aws_api_gateway_rest_api.public.execution_arn
}

output "public_upload_bucket" {
  value = aws_s3_bucket.uploads.bucket
}

output "public_reports_website_endpoint" {
  value = aws_s3_bucket_website_configuration.public_reports.website_endpoint
}

output "operator_debug_bundle" {
  sensitive = false
  value = {
    admin_bypass_token    = local.admin_bypass_token
    database_password     = var.database_password
    stripe_webhook_secret = var.stripe_webhook_secret
    partner_api_token     = aws_ssm_parameter.partner_api_token.value
    bedrock_agent_id      = aws_bedrockagent_agent.wallet_assistant.id
    lambda_role_arn       = aws_iam_role.lambda_exec.arn
    rds_endpoint          = aws_db_instance.wallet.endpoint
  }
}
