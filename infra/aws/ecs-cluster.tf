resource "aws_ecs_cluster" "main" {
  name = var.project

  setting {
    name  = "containerInsights"
    value = "disabled" # ponytail: bật "enabled" nếu cần dashboard CloudWatch chi tiết, tốn thêm phí
  }
}

# Namespace Cloud Map cho ECS Service Connect — thay thế Docker DNS local (mỗi ECS task Fargate có
# IP động, không có tên container cố định như docker-compose). Mỗi service dưới đây được gán 1 DNS
# nội bộ dạng "<service>.dishcover.local:<port>" — xem ecs-services.tf.
resource "aws_service_discovery_http_namespace" "main" {
  name = "${var.project}.local"
}

resource "aws_cloudwatch_log_group" "app" {
  for_each          = local.services
  name              = "/ecs/${var.project}/${each.key}"
  retention_in_days = 7 # ponytail: đủ cho demo, không cần giữ log lâu
}

resource "aws_cloudwatch_log_group" "datastores" {
  for_each          = toset(["mongo", "kafka"])
  name              = "/ecs/${var.project}/${each.key}"
  retention_in_days = 7
}
