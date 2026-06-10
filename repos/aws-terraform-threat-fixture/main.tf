locals {
  app_name           = "wallet-ingest-fixture"
  admin_bypass_token = "admin-fixture-token-please-change"
  bedrock_model_id   = "anthropic.claude-3-haiku-20240307-v1:0"
}

resource "aws_security_group" "wide_open" {
  name        = "${local.app_name}-wide-open"
  description = "Fixture security group with public ingress"
  vpc_id      = var.vpc_id

  ingress {
    description = "all inbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_iam_role" "lambda_exec" {
  name = "${local.app_name}-lambda-exec"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = [
            "lambda.amazonaws.com",
            "apigateway.amazonaws.com"
          ]
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy" "lambda_wildcard" {
  name = "${local.app_name}-wildcard"
  role = aws_iam_role.lambda_exec.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "WildcardAccountAccess"
        Effect = "Allow"
        Action = [
          "s3:*",
          "dynamodb:*",
          "sqs:*",
          "secretsmanager:GetSecretValue",
          "ssm:GetParameter",
          "bedrock:InvokeModel",
          "bedrock:InvokeModelWithResponseStream",
          "kms:Decrypt",
          "iam:PassRole",
          "logs:*"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_s3_bucket" "uploads" {
  bucket        = "wallet-fixture-public-uploads-2099"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_ownership_controls" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "uploads" {
  depends_on = [
    aws_s3_bucket_ownership_controls.uploads,
    aws_s3_bucket_public_access_block.uploads
  ]

  bucket = aws_s3_bucket.uploads.id
  acl    = "public-read"
}

resource "aws_s3_bucket_policy" "uploads_public" {
  bucket = aws_s3_bucket.uploads.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "PublicObjectAccess"
        Effect    = "Allow"
        Principal = "*"
        Action = [
          "s3:GetObject",
          "s3:PutObject"
        ]
        Resource = "${aws_s3_bucket.uploads.arn}/*"
      }
    ]
  })
}

resource "aws_s3_bucket_versioning" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  versioning_configuration {
    status = "Suspended"
  }
}

resource "aws_s3_bucket" "public_reports" {
  bucket        = "wallet-fixture-public-reports-2099"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "public_reports" {
  bucket = aws_s3_bucket.public_reports.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_ownership_controls" "public_reports" {
  bucket = aws_s3_bucket.public_reports.id

  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "public_reports" {
  depends_on = [
    aws_s3_bucket_ownership_controls.public_reports,
    aws_s3_bucket_public_access_block.public_reports
  ]

  bucket = aws_s3_bucket.public_reports.id
  acl    = "public-read"
}

resource "aws_s3_bucket_policy" "public_reports" {
  bucket = aws_s3_bucket.public_reports.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "PublicReportsRead"
        Effect    = "Allow"
        Principal = "*"
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.public_reports.arn}/*"
      }
    ]
  })
}

resource "aws_s3_bucket_website_configuration" "public_reports" {
  bucket = aws_s3_bucket.public_reports.id

  index_document {
    suffix = "index.html"
  }

  error_document {
    key = "error.html"
  }
}

resource "aws_ssm_parameter" "partner_api_token" {
  name        = "/wallet-fixture/partner/api-token"
  description = "Fixture partner API token stored as plaintext String"
  type        = "String"
  value       = "sk_live_parameterstore_plaintext_fixture"
}

resource "aws_dynamodb_table" "billing_events" {
  name         = "${local.app_name}-billing-events"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "event_id"

  attribute {
    name = "event_id"
    type = "S"
  }

  point_in_time_recovery {
    enabled = false
  }

  server_side_encryption {
    enabled = false
  }
}

resource "aws_sqs_queue" "jobs" {
  name                      = "${local.app_name}-jobs"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = false
}

resource "aws_db_subnet_group" "public" {
  name       = "${local.app_name}-public-db"
  subnet_ids = var.public_subnet_ids
}

resource "aws_db_instance" "wallet" {
  identifier              = "${local.app_name}-db"
  allocated_storage       = 20
  engine                  = "postgres"
  instance_class          = "db.t3.micro"
  db_name                 = "wallet"
  username                = "wallet_admin"
  password                = var.database_password
  db_subnet_group_name    = aws_db_subnet_group.public.name
  vpc_security_group_ids  = [aws_security_group.wide_open.id]
  publicly_accessible     = true
  storage_encrypted       = false
  backup_retention_period = 0
  skip_final_snapshot     = true
  deletion_protection     = false
  multi_az                = false
}

resource "aws_cloudwatch_log_group" "lambda" {
  name = "/aws/lambda/${local.app_name}-handler"
}

resource "aws_iam_role" "bedrock_agent" {
  name = "${local.app_name}-bedrock-agent"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "bedrock.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy" "bedrock_agent" {
  name = "${local.app_name}-bedrock-agent-policy"
  role = aws_iam_role.bedrock_agent.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "bedrock:InvokeModel",
          "bedrock:InvokeModelWithResponseStream",
          "lambda:InvokeFunction"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_bedrockagent_agent" "wallet_assistant" {
  agent_name                  = "${local.app_name}-wallet-assistant"
  agent_resource_role_arn     = aws_iam_role.bedrock_agent.arn
  foundation_model            = local.bedrock_model_id
  idle_session_ttl_in_seconds = 3600
  instruction                 = "Answer wallet support questions using raw customer context and operator actions."
}

resource "aws_lambda_function" "handler" {
  function_name = "${local.app_name}-handler"
  role          = aws_iam_role.lambda_exec.arn
  runtime       = "nodejs20.x"
  handler       = "index.handler"
  filename      = "build/wallet-webhook.zip"
  timeout       = 30

  environment {
    variables = {
      ADMIN_BYPASS_TOKEN       = local.admin_bypass_token
      STRIPE_WEBHOOK_SECRET    = var.stripe_webhook_secret
      DATABASE_PASSWORD        = var.database_password
      DATABASE_ENDPOINT        = aws_db_instance.wallet.endpoint
      DDB_TABLE                = aws_dynamodb_table.billing_events.name
      JOB_QUEUE_URL            = aws_sqs_queue.jobs.url
      UPLOAD_BUCKET            = aws_s3_bucket.uploads.id
      PUBLIC_REPORTS_BUCKET    = aws_s3_bucket.public_reports.id
      PARTNER_API_TOKEN_PARAM  = aws_ssm_parameter.partner_api_token.name
      BEDROCK_AGENT_ID         = aws_bedrockagent_agent.wallet_assistant.id
      BEDROCK_MODEL_ID         = local.bedrock_model_id
      CALLBACK_URL             = "http://169.254.169.254/latest/meta-data/"
    }
  }

  tracing_config {
    mode = "PassThrough"
  }

  vpc_config {
    subnet_ids         = var.public_subnet_ids
    security_group_ids = [aws_security_group.wide_open.id]
  }

  depends_on = [
    aws_cloudwatch_log_group.lambda,
    aws_iam_role_policy.lambda_wildcard
  ]
}

resource "aws_api_gateway_rest_api" "public" {
  name = "${local.app_name}-public-api"

  endpoint_configuration {
    types = ["REGIONAL"]
  }
}

resource "aws_api_gateway_resource" "proxy" {
  rest_api_id = aws_api_gateway_rest_api.public.id
  parent_id   = aws_api_gateway_rest_api.public.root_resource_id
  path_part   = "{proxy+}"
}

resource "aws_api_gateway_method" "any_proxy" {
  rest_api_id      = aws_api_gateway_rest_api.public.id
  resource_id      = aws_api_gateway_resource.proxy.id
  http_method      = "ANY"
  authorization    = "NONE"
  api_key_required = false
}

resource "aws_api_gateway_integration" "lambda_proxy" {
  rest_api_id             = aws_api_gateway_rest_api.public.id
  resource_id             = aws_api_gateway_resource.proxy.id
  http_method             = aws_api_gateway_method.any_proxy.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.handler.invoke_arn
}

resource "aws_api_gateway_deployment" "public" {
  rest_api_id = aws_api_gateway_rest_api.public.id

  triggers = {
    redeploy = sha1(jsonencode([
      aws_api_gateway_resource.proxy.id,
      aws_api_gateway_method.any_proxy.id,
      aws_api_gateway_integration.lambda_proxy.id
    ]))
  }
}

resource "aws_api_gateway_stage" "prod" {
  deployment_id        = aws_api_gateway_deployment.public.id
  rest_api_id          = aws_api_gateway_rest_api.public.id
  stage_name           = "prod"
  xray_tracing_enabled = false
}

resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowPublicApiGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.handler.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.public.execution_arn}/*/*"
}

resource "aws_cloudwatch_event_rule" "nightly_settlement" {
  name                = "${local.app_name}-nightly-settlement"
  schedule_expression = "rate(5 minutes)"
}

resource "aws_cloudwatch_event_target" "nightly_settlement" {
  rule = aws_cloudwatch_event_rule.nightly_settlement.name
  arn  = aws_lambda_function.handler.arn

  input = jsonencode({
    action     = "settle-all-wallets"
    adminToken = local.admin_bypass_token
    callback   = "http://169.254.169.254/latest/meta-data/"
  })
}

resource "aws_lambda_permission" "eventbridge" {
  statement_id  = "AllowEventBridgeInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.handler.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.nightly_settlement.arn
}
