resource "aws_db_subnet_group" "main" {
  name       = "${var.project}-db-subnets"
  subnet_ids = data.aws_subnets.default.ids
  tags       = { Project = var.project }
}

resource "aws_db_instance" "postgres" {
  identifier     = "${var.project}-pg"
  engine         = "postgres"
  engine_version = "16"
  instance_class = var.db_instance_class

  allocated_storage = 20
  storage_type      = "gp3"

  db_name  = "larder"
  username = "larder_app"
  password = var.postgres_master_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.internal.id]
  publicly_accessible    = false

  # ponytail: demo only — không cần Multi-AZ/backup dài hạn. Bật lại nếu chạy production thật.
  multi_az                = false
  backup_retention_period = 0
  skip_final_snapshot     = true
  deletion_protection     = false

  tags = { Project = var.project }
}

output "rds_endpoint" {
  value = aws_db_instance.postgres.address
}
