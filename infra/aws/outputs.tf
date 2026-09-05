output "next_steps" {
  value = <<-EOT
    1. Đẩy image lên ECR: xem .github/workflows/deploy.yml (build + push tự động khi chạy workflow)
    2. GitHub → Settings → Secrets and variables → Actions, thêm:
       - AWS_DEPLOY_ROLE_ARN = ${aws_iam_role.github_deploy.arn}
    3. Base URL API: http://${aws_lb.main.dns_name}
    4. Frontend: https://${aws_cloudfront_distribution.frontend.domain_name}
    5. Chạy backfill schema Postgres 1 lần (RDS ${aws_db_instance.postgres.address} không tự chạy
       init-schemas.sql như container local): psql vào và chạy docker-setup/init-schemas.sql thủ công.
  EOT
}
