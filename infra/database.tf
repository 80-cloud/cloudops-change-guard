resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${var.project_name}-db"
  }
}

resource "aws_security_group" "rds" {
  name        = "${var.project_name}-rds"
  description = "Restrict RDS PostgreSQL access to the VPC"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-rds"
  }
}

resource "aws_vpc_security_group_ingress_rule" "rds_postgres" {
  security_group_id = aws_security_group.rds.id
  description       = "Allow PostgreSQL from within the VPC"
  cidr_ipv4         = var.vpc_cidr
  from_port         = 5432
  to_port           = 5432
  ip_protocol       = "tcp"
}

resource "aws_db_instance" "main" {
  identifier     = "${var.project_name}-db"
  engine         = "postgres"
  engine_version = var.db_engine_version
  instance_class = var.db_instance_class

  allocated_storage = var.db_allocated_storage
  storage_encrypted = true
  kms_key_id        = aws_kms_key.main.arn

  db_name  = var.db_name
  username = var.db_username

  manage_master_user_password   = true
  master_user_secret_kms_key_id = aws_kms_key.main.arn

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period             = 7
  deletion_protection                 = true
  iam_database_authentication_enabled = true

  performance_insights_enabled    = true
  performance_insights_kms_key_id = aws_kms_key.main.arn

  auto_minor_version_upgrade = true
  apply_immediately          = false

  lifecycle {
    prevent_destroy = true
  }
}
