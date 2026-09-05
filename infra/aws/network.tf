# Dùng VPC mặc định của account — đơn giản nhất cho demo, không tự dựng VPC/NAT riêng
# (NAT Gateway ~$33/tháng, không đáng cho nhu cầu demo — xem docs/aws-deployment-guide.md).
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

resource "aws_security_group" "public" {
  # ponytail: AWS chi cho ASCII trong description (regex khong nhan ky tu co dau/em-dash) - cac
  # dong description trong file nay co chu de ASCII thuan tuy vi ly do do, khac voi comment HCL
  # (bat dau bang #) van dung tieng Viet co dau binh thuong.
  name        = "${var.project}-public"
  description = "ALB - nhan traffic tu Internet"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "HTTP (redirect to HTTPS, see alb.tf)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Project = var.project }
}

# 1 security group dùng chung cho mọi service nội bộ + RDS + Mongo + Kafka (self-referencing —
# ponytail: đơn giản hơn 1 SG riêng cho từng cặp service, đủ an toàn vì tất cả đều là tài nguyên
# của cùng 1 project, không có tenant khác trong VPC).
resource "aws_security_group" "internal" {
  name        = "${var.project}-internal"
  description = "Internal traffic - ECS service, RDS, Mongo, Kafka"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "ALB -> Gateway"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.public.id]
  }

  ingress {
    description = "Everything in this SG talks to each other (service-to-service, Postgres 5432, Mongo 27017, Kafka 9092)"
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    self        = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Project = var.project }
}
