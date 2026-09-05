# Chay 1 lan duy nhat sau khi terraform apply xong lan dau - tao 4 schema Postgres tren RDS.
# RDS dang private (dung thiet ke) nen chay qua 1 task ECS tam ben trong VPC, khong mo RDS ra internet.
# Xoa file bootstrap-taskdef.json o cuoi vi no chua mat khau DB dang plaintext.

$ErrorActionPreference = "Stop"
$Region = "ap-southeast-1"

Write-Host "Lay thong tin can thiet..."
$PgPass = aws secretsmanager get-secret-value --secret-id dishcover/postgres-password --query SecretString --output text --region $Region
$Subnet = aws ec2 describe-subnets --filters "Name=default-for-az,Values=true" --region $Region --query "Subnets[0].SubnetId" --output text
$Sg = aws ec2 describe-security-groups --filters "Name=group-name,Values=dishcover-internal" --region $Region --query "SecurityGroups[0].GroupId" --output text
$RdsHost = aws rds describe-db-instances --db-instance-identifier dishcover-pg --region $Region --query "DBInstances[0].Endpoint.Address" --output text

$Sql = "CREATE SCHEMA IF NOT EXISTS user_service; CREATE SCHEMA IF NOT EXISTS inventory_service; CREATE SCHEMA IF NOT EXISTS matching_service; CREATE SCHEMA IF NOT EXISTS notification_service; CREATE EXTENSION IF NOT EXISTS vector;"

$TaskDef = @"
{
  "family": "dishcover-db-bootstrap",
  "requiresCompatibilities": ["FARGATE"],
  "networkMode": "awsvpc",
  "cpu": "256",
  "memory": "512",
  "executionRoleArn": "arn:aws:iam::640012953073:role/dishcover-ecs-execution",
  "containerDefinitions": [{
    "name": "bootstrap",
    "image": "postgres:16-alpine",
    "command": ["psql", "-h", "$RdsHost", "-U", "larder_app", "-d", "larder", "-c", "$Sql"],
    "environment": [{"name": "PGPASSWORD", "value": "$PgPass"}],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/ecs/dishcover/db-bootstrap",
        "awslogs-region": "$Region",
        "awslogs-stream-prefix": "bootstrap",
        "awslogs-create-group": "true"
      }
    }
  }]
}
"@

# QUAN TRONG: Out-File -Encoding utf8 tren Windows PowerShell 5.1 ghi kem BOM, lam AWS CLI bao
# "Invalid JSON received" - dung .NET WriteAllText de ghi UTF-8 KHONG BOM.
$JsonPath = Join-Path (Get-Location) "bootstrap-taskdef.json"
[System.IO.File]::WriteAllText($JsonPath, $TaskDef)

Write-Host "Dang ky task definition..."
aws ecs register-task-definition --cli-input-json "file://$JsonPath" --region $Region | Out-Null

# dishcover-ecs-execution chi co AmazonECSTaskExecutionRolePolicy (managed) - KHONG co
# logs:CreateLogGroup, nen "awslogs-create-group": "true" trong task def se fail
# (AccessDeniedException) va task khong start duoc. Tu tao log group truoc bang quyen CLI
# cua chinh nguoi dung (rong hon), bo qua loi neu da ton tai san.
Write-Host "Tao log group (bo qua neu da co)..."
aws logs create-log-group --log-group-name "/ecs/dishcover/db-bootstrap" --region $Region 2>$null

Write-Host "Chay task (mat ~30-60s)..."
$TaskArn = aws ecs run-task --cluster dishcover --task-definition dishcover-db-bootstrap `
  --launch-type FARGATE `
  --network-configuration "awsvpcConfiguration={subnets=[$Subnet],securityGroups=[$Sg],assignPublicIp=ENABLED}" `
  --region $Region --query "tasks[0].taskArn" --output text

if ([string]::IsNullOrWhiteSpace($TaskArn) -or $TaskArn -eq "None") {
  Write-Host "LOI: khong lay duoc TaskArn, dung lai. Kiem tra log AWS CLI o tren."
  Remove-Item $JsonPath -Force -ErrorAction SilentlyContinue
  exit 1
}

Write-Host "Task: $TaskArn"
Write-Host "Doi task chay xong..."
aws ecs wait tasks-stopped --cluster dishcover --tasks $TaskArn --region $Region

$ExitCode = aws ecs describe-tasks --cluster dishcover --tasks $TaskArn --region $Region --query "tasks[0].containers[0].exitCode" --output text
Write-Host "Exit code: $ExitCode (0 = thanh cong)"

Write-Host "--- Log ---"
$TaskId = $TaskArn.Split("/")[-1]
Start-Sleep -Seconds 5
aws logs get-log-events --log-group-name "/ecs/dishcover/db-bootstrap" --log-stream-name "bootstrap/bootstrap/$TaskId" --region $Region --query "events[].message" --output text

Write-Host "Don dep file chua mat khau..."
Remove-Item $JsonPath -Force -ErrorAction SilentlyContinue

Write-Host "XONG. Neu exit code = 0, 4 schema da tao tren RDS."
