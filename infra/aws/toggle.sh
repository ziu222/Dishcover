#!/usr/bin/env bash
# Bật/tắt hạ tầng demo để tránh trả tiền 24/7 khi không dùng.
# Dùng: ./toggle.sh stop | ./toggle.sh start
#
# stop:  scale 10 ECS service (8 app + mongo + kafka) về desired_count=0 + dừng RDS
#        -> hết tiền ECS Fargate ngay, RDS chỉ còn phí storage (~$2/tháng cho 20GB gp3).
# start: bật RDS trước (mất 2-5 phút "available"), rồi scale ECS service lại về 1.
#
# LƯU Ý: AWS tự động BẬT LẠI RDS đã stop sau 7 ngày (giới hạn cứng, không tắt được) — nếu để quên
# hơn 1 tuần, RDS sẽ tự chạy lại và tính phí, ALB (~$16/tháng) không tắt được bằng cách này (không
# scale theo desired_count) — muốn hết cả phí ALB thì phải terraform destroy.

set -euo pipefail

CLUSTER="dishcover"
DB_INSTANCE="dishcover-pg"
SERVICES=(gateway user inventory recipe matching rag image notification mongo kafka)

action="${1:-}"
if [[ "$action" != "start" && "$action" != "stop" ]]; then
  echo "Dùng: $0 start|stop" >&2
  exit 1
fi

if [[ "$action" == "stop" ]]; then
  echo "Scale ${#SERVICES[@]} ECS service về 0..."
  for svc in "${SERVICES[@]}"; do
    aws ecs update-service --cluster "$CLUSTER" --service "$svc" --desired-count 0 >/dev/null
    echo "  - $svc -> 0"
  done
  echo "Dừng RDS $DB_INSTANCE..."
  aws rds stop-db-instance --db-instance-identifier "$DB_INSTANCE" >/dev/null
  echo "Xong. ALB vẫn chạy (không tắt được theo cách này, ~\$0.53/ngày) — dùng 'terraform destroy' nếu cần hết sạch phí."
else
  echo "Khởi động RDS $DB_INSTANCE (mất vài phút)..."
  aws rds start-db-instance --db-instance-identifier "$DB_INSTANCE" >/dev/null
  aws rds wait db-instance-available --db-instance-identifier "$DB_INSTANCE"
  echo "RDS available. Scale ${#SERVICES[@]} ECS service về 1..."
  for svc in "${SERVICES[@]}"; do
    aws ecs update-service --cluster "$CLUSTER" --service "$svc" --desired-count 1 >/dev/null
    echo "  - $svc -> 1"
  done
  echo "Đợi service ổn định (có thể mất 2-3 phút, Spring Boot khởi động)..."
  aws ecs wait services-stable --cluster "$CLUSTER" --services "${SERVICES[@]}"
  echo "Xong. Base URL: xem 'terraform output alb_dns_name'."
fi
