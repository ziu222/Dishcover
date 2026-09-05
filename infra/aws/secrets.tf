# Giá trị thật lấy từ variables (sensitive) — không hardcode. Set qua terraform.tfvars (gitignored)
# hoặc TF_VAR_xxx trong CI. ECS task đọc qua "secrets" trong task definition (Secrets Manager tự
# inject lúc container start, không hiện trong log/console) — không dùng "environment" cho các key này.

resource "aws_secretsmanager_secret" "jwt_secret" {
  name = "${var.project}/jwt-secret"
}
resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = var.jwt_secret
}

resource "aws_secretsmanager_secret" "internal_service_secret" {
  name = "${var.project}/internal-service-secret"
}
resource "aws_secretsmanager_secret_version" "internal_service_secret" {
  secret_id     = aws_secretsmanager_secret.internal_service_secret.id
  secret_string = var.internal_service_secret
}

resource "aws_secretsmanager_secret" "postgres_password" {
  name = "${var.project}/postgres-password"
}
resource "aws_secretsmanager_secret_version" "postgres_password" {
  secret_id     = aws_secretsmanager_secret.postgres_password.id
  secret_string = var.postgres_master_password
}

resource "aws_secretsmanager_secret" "mongo_password" {
  name = "${var.project}/mongo-password"
}
resource "aws_secretsmanager_secret_version" "mongo_password" {
  secret_id     = aws_secretsmanager_secret.mongo_password.id
  secret_string = var.mongo_root_password
}

resource "aws_secretsmanager_secret" "gemini_api_key" {
  name = "${var.project}/gemini-api-key"
}
resource "aws_secretsmanager_secret_version" "gemini_api_key" {
  secret_id     = aws_secretsmanager_secret.gemini_api_key.id
  secret_string = var.gemini_api_key
}

resource "aws_secretsmanager_secret" "openai_api_key" {
  name = "${var.project}/openai-api-key"
}
resource "aws_secretsmanager_secret_version" "openai_api_key" {
  secret_id     = aws_secretsmanager_secret.openai_api_key.id
  secret_string = var.openai_api_key
}

resource "aws_secretsmanager_secret" "mail_password" {
  name = "${var.project}/mail-password"
}
resource "aws_secretsmanager_secret_version" "mail_password" {
  secret_id     = aws_secretsmanager_secret.mail_password.id
  secret_string = var.mail_password
}

resource "aws_secretsmanager_secret" "turnstile_secret_key" {
  name = "${var.project}/turnstile-secret-key"
}
resource "aws_secretsmanager_secret_version" "turnstile_secret_key" {
  secret_id     = aws_secretsmanager_secret.turnstile_secret_key.id
  secret_string = var.turnstile_secret_key
}
