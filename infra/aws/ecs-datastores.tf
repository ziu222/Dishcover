# Mongo + Kafka tự host trong ECS Fargate (thay vì DocumentDB/MSK managed) — quyết định chi phí cho
# mục đích demo, xem cảnh báo chi phí ở docs/aws-deployment-guide.md. Ephemeral storage (mất dữ liệu
# khi task restart) — chấp nhận được cho demo, KHÔNG dùng cho production thật.

resource "aws_ecs_task_definition" "mongo" {
  family                   = "${var.project}-mongo"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name         = "mongo"
    image        = "mongo:7"
    portMappings = [{ name = "mongo", containerPort = 27017 }]
    environment = [
      { name = "MONGO_INITDB_ROOT_USERNAME", value = "larder_app" },
      { name = "MONGO_INITDB_DATABASE", value = "recipe_matcher_db" },
    ]
    secrets = [
      { name = "MONGO_INITDB_ROOT_PASSWORD", valueFrom = aws_secretsmanager_secret.mongo_password.arn },
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.datastores["mongo"].name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "mongo"
      }
    }
  }])
}

resource "aws_ecs_service" "mongo" {
  name            = "mongo"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.mongo.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.internal.id]
    assign_public_ip = true # subnet mặc định là public; task cần IP public để pull image từ Docker Hub (không dùng NAT Gateway)
  }

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.main.arn
    service {
      port_name      = "mongo"
      discovery_name = "mongo"
      client_alias {
        port = 27017
      }
    }
  }
}

resource "aws_ecs_task_definition" "kafka" {
  family                   = "${var.project}-kafka"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  # Cấu hình KRaft single-broker y hệt docker-setup/docker-compose.yml — advertised listener phải
  # trỏ đúng DNS Service Connect ("kafka.dishcover.local") để các service khác resolve được, khác
  # với "localhost" dùng lúc chạy local.
  container_definitions = jsonencode([{
    name         = "kafka"
    image        = "apache/kafka:3.8.0"
    portMappings = [{ name = "kafka", containerPort = 9092 }]
    environment = [
      { name = "KAFKA_NODE_ID", value = "1" },
      { name = "KAFKA_PROCESS_ROLES", value = "broker,controller" },
      { name = "KAFKA_LISTENERS", value = "PLAINTEXT://:9092,CONTROLLER://:9093" },
      { name = "KAFKA_ADVERTISED_LISTENERS", value = "PLAINTEXT://kafka.${var.project}.local:9092" },
      { name = "KAFKA_CONTROLLER_QUORUM_VOTERS", value = "1@localhost:9093" },
      { name = "KAFKA_CONTROLLER_LISTENER_NAMES", value = "CONTROLLER" },
      { name = "KAFKA_INTER_BROKER_LISTENER_NAME", value = "PLAINTEXT" },
      { name = "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", value = "1" },
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.datastores["kafka"].name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "kafka"
      }
    }
  }])
}

resource "aws_ecs_service" "kafka" {
  name            = "kafka"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.kafka.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.internal.id]
    assign_public_ip = true
  }

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.main.arn
    service {
      port_name      = "kafka"
      discovery_name = "kafka"
      client_alias {
        port = 9092
      }
    }
  }
}
