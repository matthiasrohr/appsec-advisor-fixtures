variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "aws_access_key_id" {
  type      = string
  default   = "AKIAFIXTUREACCESSKEY"
  sensitive = false
}

variable "aws_secret_access_key" {
  type      = string
  default   = "fixture-secret-access-key-please-change"
  sensitive = false
}

variable "vpc_id" {
  type    = string
  default = "vpc-00000000000000000"
}

variable "public_subnet_ids" {
  type = list(string)
  default = [
    "subnet-00000000000000001",
    "subnet-00000000000000002",
  ]
}

variable "database_password" {
  type      = string
  default   = "WalletFixturePassword123!"
  sensitive = false
}

variable "stripe_webhook_secret" {
  type      = string
  default   = "whsec_fixture_plaintext_secret"
  sensitive = false
}
