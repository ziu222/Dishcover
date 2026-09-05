# ponytail: mọi service nhận CÙNG 1 bộ env var/secret đầy đủ (kể cả biến nó không dùng tới) thay vì
# tự khai báo riêng cho từng service — image không đọc biến thừa nào cả (Spring chỉ bind biến có
# @Value/${...} khớp), đổi lại Terraform ngắn hơn nhiều so với 8 khối container_definitions khác
# nhau. Nếu sau này cần tách quyền đọc secret theo từng service, tách execution role riêng lúc đó.

locals {
  service_urls = {
    for name, s in local.services :
    "${upper(name)}_SERVICE_URL" => "http://${name}.${var.project}.local:${s.port}"
  }

  common_env = [
    for k, v in local.service_urls : { name = k, value = v }
  ]

  common_secrets = [
    { name = "JWT_SECRET", valueFrom = aws_secretsmanager_secret.jwt_secret.arn },
    { name = "INTERNAL_SERVICE_SECRET", valueFrom = aws_secretsmanager_secret.internal_service_secret.arn },
    { name = "POSTGRES_PASSWORD", valueFrom = aws_secretsmanager_secret.postgres_password.arn },
    { name = "MONGO_ROOT_PASSWORD", valueFrom = aws_secretsmanager_secret.mongo_password.arn },
    { name = "GEMINI_API_KEY", valueFrom = aws_secretsmanager_secret.gemini_api_key.arn },
    { name = "OPENAI_API_KEY", valueFrom = aws_secretsmanager_secret.openai_api_key.arn },
    { name = "MAIL_PASSWORD", valueFrom = aws_secretsmanager_secret.mail_password.arn },
    { name = "TURNSTILE_SECRET_KEY", valueFrom = aws_secretsmanager_secret.turnstile_secret_key.arn },
  ]
}

resource "aws_ecs_task_definition" "app" {
  for_each                 = local.services
  family                   = "${var.project}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = coalesce(each.value.cpu, var.app_cpu)
  memory                   = coalesce(each.value.memory, var.app_memory)
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name         = each.key
    image        = "${aws_ecr_repository.services[each.key].repository_url}:${var.image_tag}"
    portMappings = [{ name = each.key, containerPort = each.value.port }]

    environment = concat(local.common_env, [
      { name = "POSTGRES_DB", value = "larder" },
      { name = "POSTGRES_USER", value = "larder_app" },
      { name = "MONGO_DB", value = "recipe_matcher_db" },
      { name = "MONGO_ROOT_USER", value = "larder_app" },
      { name = "MONGO_HOST", value = "mongo.${var.project}.local" },
      { name = "KAFKA_BOOTSTRAP_SERVERS", value = "kafka.${var.project}.local:9092" },
      # RDS chỉ có 1 host cho cả 4 schema (Database-per-Service ở mức schema, không phải instance
      # riêng — đúng "Phương án B" đã chốt trong CLAUDE.md mục 3) — mỗi service tự thêm
      # ?currentSchema=<tên> trong application.yml, không cần biến riêng ở đây.
      { name = "PGHOST", value = aws_db_instance.postgres.address },
      { name = "MAIL_USERNAME", value = var.mail_username },
      { name = "FRONTEND_URL", value = "https://www.${var.domain_name}" },
    ])
    secrets = local.common_secrets

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.app[each.key].name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = each.key
      }
    }
  }])
}

resource "aws_ecs_service" "app" {
  for_each        = local.services
  name            = each.key
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app[each.key].arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.internal.id]
    assign_public_ip = true # subnet mặc định public; cần IP public để gọi Gemini/OpenAI/Cloudinary ra Internet (không dùng NAT Gateway)
  }

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.main.arn
    service {
      port_name      = each.key
      discovery_name = each.key
      client_alias {
        port = each.value.port
      }
    }
  }

  # Chỉ Gateway gắn Load Balancer — đúng nguyên tắc "Private Network: chỉ API Gateway expose port
  # ra ngoài" đã ghi trong CLAUDE.md mục 3, giờ thực hiện bằng ALB thay vì docker-compose `ports:`.
  dynamic "load_balancer" {
    for_each = each.value.public == true ? [1] : []
    content {
      target_group_arn = aws_lb_target_group.gateway.arn
      container_name   = each.key
      container_port   = each.value.port
    }
  }

  depends_on = [aws_ecs_service.mongo, aws_ecs_service.kafka]
}
