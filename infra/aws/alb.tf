resource "aws_lb" "main" {
  name               = "${var.project}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.public.id]
  subnets            = data.aws_subnets.default.ids
}

resource "aws_lb_target_group" "gateway" {
  name        = "${var.project}-gateway"
  port        = local.services.gateway.port
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "ip" # bắt buộc cho Fargate awsvpc mode

  health_check {
    path                = "/actuator/health"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 15
    timeout             = 5
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.gateway.arn
  }

  # ponytail: HTTP-only cho demo (không có domain/ACM certificate). Deploy thật cần thêm listener
  # 443 + aws_acm_certificate + chuyển default_action ở đây thành redirect HTTP->HTTPS.
}

output "alb_dns_name" {
  value       = aws_lb.main.dns_name
  description = "Base URL API — vd http://<giá trị này>/user-service/auth/login"
}
