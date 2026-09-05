output "next_steps" {
  value = <<-EOT
    1. Thêm 2 CNAME validate ACM vào Hostinger TRƯỚC (xem output acm_validation_records) — apply
       treo ở đây cho tới khi thấy record, có thể mất 5-30 phút để DNS lan truyền + ACM nhận diện.
    2. Sau khi apply xong, thêm 2 CNAME cuối vào Hostinger:
       - api.${var.domain_name}  -> ${aws_lb.main.dns_name}
       - www.${var.domain_name}  -> ${aws_cloudfront_distribution.frontend.domain_name}
    3. GitHub → Settings → Secrets and variables → Actions, thêm:
       - AWS_DEPLOY_ROLE_ARN = ${aws_iam_role.github_deploy.arn}
       - ALB_BASE_URL = https://api.${var.domain_name}
    4. Đẩy image lên ECR + deploy: chạy workflow "Deploy to AWS" (tab Actions)
    5. API thật: https://api.${var.domain_name} — Frontend: https://www.${var.domain_name}
    6. Chạy backfill schema Postgres 1 lần (RDS ${aws_db_instance.postgres.address} không tự chạy
       init-schemas.sql như container local): psql vào và chạy docker-setup/init-schemas.sql thủ công.
  EOT
}
