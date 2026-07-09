data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}

data "aws_iam_policy_document" "app_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "app" {
  name               = "${var.project_name}-app"
  assume_role_policy = data.aws_iam_policy_document.app_assume.json
}

resource "aws_iam_role_policy_attachment" "app_ssm" {
  role       = aws_iam_role.app.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "app" {
  name = "${var.project_name}-app"
  role = aws_iam_role.app.name
}

resource "aws_security_group" "app" {
  name        = "${var.project_name}-app"
  description = "App EC2 web access and SSM egress"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-app"
  }
}

# 公開Webエンドポイントとして 80/443 を意図的に開放する（tfsec の public-ingress 指摘は設計通り）。
# tfsec:ignore:aws-ec2-no-public-ingress-sgr
resource "aws_vpc_security_group_ingress_rule" "app_http_in" {
  security_group_id = aws_security_group.app.id
  description       = "HTTP from internet (redirect to HTTPS and ACME http-01)"
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

# tfsec:ignore:aws-ec2-no-public-ingress-sgr
resource "aws_vpc_security_group_ingress_rule" "app_https_in" {
  security_group_id = aws_security_group.app.id
  description       = "HTTPS from internet"
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "app_https" {
  security_group_id = aws_security_group.app.id
  description       = "HTTPS egress for SSM, secrets and package fetch"
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "app_http" {
  security_group_id = aws_security_group.app.id
  description       = "HTTP egress for OS package mirrors"
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

# アプリから RDS PostgreSQL への外向き接続を許可（RDS SG 宛のみ）。
# これが無いと EC2 は DB へ接続できず、起動時に接続タイムアウトで停止する。
resource "aws_vpc_security_group_egress_rule" "app_postgres" {
  security_group_id            = aws_security_group.app.id
  description                  = "PostgreSQL egress to the RDS security group only"
  referenced_security_group_id = aws_security_group.rds.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

resource "aws_instance" "app" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.instance_type
  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.app.id]
  iam_instance_profile        = aws_iam_instance_profile.app.name
  associate_public_ip_address = true

  # user_data は「公開リポを clone → provision.sh 実行」だけの薄い起動役。
  # 重い配線（runtime/nginx/systemd/秘密取得）は infra/deploy/provision.sh に集約する。
  user_data = <<-BOOT
    #!/bin/bash
    set -eux
    export PROJECT_NAME=${var.project_name}
    export AWS_REGION=${var.region}
    dnf install -y git
    rm -rf /opt/deploy-src
    git clone --depth 1 --branch ${var.app_repo_branch} ${var.app_repo_url} /opt/deploy-src
    chmod +x /opt/deploy-src/infra/deploy/provision.sh
    /opt/deploy-src/infra/deploy/provision.sh
  BOOT

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  root_block_device {
    encrypted   = true
    kms_key_id  = aws_kms_key.main.arn
    volume_type = "gp3"
    # 最新 AL2023 AMI のルートスナップショットが 30GB のため 30 以上が必須（EC2 無料枠の EBS 30GB 内）。
    volume_size = 30
  }

  # provision.sh は起動直後に SSM パラメータと DB 秘密を取得するため、
  # それらとインスタンスロールのポリシーが揃ってから EC2 を起動させる。
  # （パラメータは RDS に依存するので、実質 RDS 完成後に EC2 が立ち上がる）
  depends_on = [
    aws_iam_role_policy.app_secrets,
    aws_iam_role_policy.app_artifacts,
    aws_ssm_parameter.jwt_secret,
    aws_ssm_parameter.db_url,
    aws_ssm_parameter.db_user,
    aws_ssm_parameter.db_secret_arn,
    aws_ssm_parameter.cors_allowed_origin,
    aws_ssm_parameter.cookie_secure,
    aws_ssm_parameter.spring_profiles_active,
    aws_ssm_parameter.seed_enabled,
    aws_ssm_parameter.admin_password,
  ]

  tags = {
    Name = "${var.project_name}-app"
  }
}

# 停止/起動でIPが変わらないよう Elastic IP を固定（DuckDNS が指す先）。
# 12か月無料枠には public IPv4 750h/月が含まれる（1台なら実質無料）。
resource "aws_eip" "app" {
  instance = aws_instance.app.id
  domain   = "vpc"

  tags = {
    Name = "${var.project_name}-app"
  }
}
