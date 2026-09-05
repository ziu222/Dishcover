# Infra AWS (Terraform) — Dishcover

ECS Fargate (8 service) + RDS Postgres + Mongo/Kafka tự host trong ECS + ALB + S3/CloudFront (frontend).
Chi tiết kiến trúc & cảnh báo chi phí: [docs/aws-deployment-guide.md](../../docs/aws-deployment-guide.md).

## Apply lần đầu (thủ công, từ máy có quyền AWS — CI KHÔNG chạy bước này)

```bash
cd infra/aws
cp terraform.tfvars.example terraform.tfvars   # điền secret thật, KHÔNG commit file này
terraform init
terraform plan      # đọc kỹ trước khi apply — tạo tài nguyên tốn phí thật
terraform apply
```

Sau khi apply xong, lấy giá trị output:
```bash
terraform output              # xem tất cả
terraform output -raw github_deploy_role_arn
terraform output -raw frontend_bucket_name
terraform output -raw cloudfront_distribution_id
terraform output -raw alb_dns_name
```

## Cấu hình GitHub Actions (Settings → Secrets and variables → Actions)

| Secret | Giá trị lấy từ |
|---|---|
| `AWS_DEPLOY_ROLE_ARN` | `terraform output -raw github_deploy_role_arn` |
| `FRONTEND_BUCKET` | `terraform output -raw frontend_bucket_name` |
| `CLOUDFRONT_DISTRIBUTION_ID` | `terraform output -raw cloudfront_distribution_id` |
| `ALB_BASE_URL` | `http://` + `terraform output -raw alb_dns_name` |

Và tạo GitHub Environment tên `production` (Settings → Environments → New environment) + bật **Required reviewers** — đây là bước approve thủ công trước khi `.github/workflows/deploy.yml` thật sự chạy (bắt buộc theo checklist devops-engineer: "MUST NOT deploy to production without explicit approval").

Sau bước này mới chạy `Deploy to AWS` từ tab Actions (workflow_dispatch thủ công, không tự chạy khi merge).

## Bootstrap DB (1 lần, sau khi RDS tạo xong)

RDS không tự chạy `init-schemas.sql` như container `postgres` local — cần chạy tay 1 lần:
```bash
psql "host=$(terraform output -raw rds_endpoint) dbname=larder user=larder_app sslmode=require" \
  -f ../../docker-setup/init-schemas.sql
```
(cần tạm cho `publicly_accessible = true` trong `rds.tf` HOẶC chạy từ 1 máy trong cùng VPC — VD 1 ECS task tạm hoặc EC2 bastion nhỏ.)

## Bật/tắt hạ tầng giữa các lần demo (tiết kiệm phí, không cần destroy/apply lại)

`terraform destroy` + `apply` lại mỗi lần demo thì mất 5-10 phút và Mongo/Kafka mất sạch dữ liệu
(ephemeral storage). Thay vào đó, **tắt = scale ECS về 0 + dừng RDS** (giữ nguyên hạ tầng, bật lại
trong ~2-3 phút, dữ liệu Postgres còn nguyên vì RDS storage không mất khi stop):

```bash
./toggle.sh stop     # trước khi rời máy / xong demo
./toggle.sh start     # trước buổi demo kế tiếp
```

Hoặc từ GitHub (điện thoại, không cần AWS CLI cục bộ): tab **Actions → Toggle Infra (start/stop) →
Run workflow**, chọn `start`/`stop` (dùng chung `AWS_DEPLOY_ROLE_ARN` đã cấu hình ở trên, không cần
secret thêm).

**Giới hạn**: ALB (~$0.5/ngày) vẫn chạy khi "stop" — không tắt theo cách này, chỉ `terraform destroy`
mới hết hẳn phí ALB. AWS cũng **tự động bật lại RDS đã stop sau 7 ngày** (giới hạn cứng của AWS,
không tắt được) — nếu quên `stop` lại quá 1 tuần, RDS tự chạy và tính phí tiếp; đặt nhắc lịch nếu
nghỉ demo dài ngày, hoặc `terraform destroy` luôn cho chắc.

## Rollback

**ECS service bị lỗi sau deploy** — quay lại task definition revision trước:
```bash
aws ecs list-task-definitions --family-prefix dishcover-<service> --sort DESC
aws ecs update-service --cluster dishcover --service <service> \
  --task-definition dishcover-<service>:<revision cũ> --force-new-deployment
aws ecs wait services-stable --cluster dishcover --services <service>
```

**Frontend lỗi sau deploy** — S3 versioning chưa bật (ponytail: thêm nếu cần rollback frontend tự động); tạm thời build lại từ commit trước rồi `npm run build && aws s3 sync ... && aws cloudfront create-invalidation`.

**Hạ tầng (Terraform) lỗi** — `terraform plan` trước khi apply luôn; nếu đã apply sai, sửa `.tf` rồi `terraform apply` lại (Terraform tự tính diff, không cần rollback thủ công trừ khi đã xoá nhầm tài nguyên có state — lúc đó cần `terraform import` lại hoặc khôi phục từ đâu terraform.tfstate backup).

## Dọn dẹp (BẮT BUỘC sau demo — xem cảnh báo chi phí)

```bash
cd infra/aws
terraform destroy
```
