data "aws_caller_identity" "current" {}

# アプリ設定の集中管理。非機密は String、機密は SecureString（共通CMKで暗号）。
# EC2 の provision がインスタンスロールで取得し、systemd の EnvironmentFile を生成する。

# トークン偽造を防ぐ本命の秘密。ランダム生成して SecureString に格納（コードに置かない）。
resource "random_password" "jwt_secret" {
  length  = 48
  special = false
}

resource "aws_ssm_parameter" "jwt_secret" {
  name   = "/${var.project_name}/jwt_secret"
  type   = "SecureString"
  key_id = aws_kms_key.main.id
  value  = random_password.jwt_secret.result
}

resource "aws_ssm_parameter" "db_url" {
  name  = "/${var.project_name}/db_url"
  type  = "String"
  value = "jdbc:postgresql://${aws_db_instance.main.address}:${aws_db_instance.main.port}/${var.db_name}"
}

resource "aws_ssm_parameter" "db_user" {
  name  = "/${var.project_name}/db_user"
  type  = "String"
  value = var.db_username
}

# RDS マスターパスワードは AWS 管理（Secrets Manager）。その ARN を箱へ伝える。
resource "aws_ssm_parameter" "db_secret_arn" {
  name  = "/${var.project_name}/db_secret_arn"
  type  = "String"
  value = aws_db_instance.main.master_user_secret[0].secret_arn
}

resource "aws_ssm_parameter" "cors_allowed_origin" {
  name = "/${var.project_name}/cors_allowed_origin"
  type = "String"
  # 空文字は SSM が拒否するため、TLS 化前は同一オリジン配信の既定値を入れておく
  # （公開後は public_origin=https://<host> を設定して上書きする）。
  value = var.public_origin == "" ? "http://localhost:5177" : var.public_origin
}

resource "aws_ssm_parameter" "cookie_secure" {
  name  = "/${var.project_name}/cookie_secure"
  type  = "String"
  value = tostring(var.cookie_secure)
}

resource "aws_ssm_parameter" "spring_profiles_active" {
  name = "/${var.project_name}/spring_profiles_active"
  type = "String"
  # 空文字は SSM が拒否するため、Mock を明示する既定値を入れておく。
  value = var.spring_profiles_active == "" ? "default" : var.spring_profiles_active
}

resource "aws_ssm_parameter" "seed_enabled" {
  name  = "/${var.project_name}/seed_enabled"
  type  = "String"
  value = tostring(var.seed_enabled)
}

# デモ管理者パスワード。公開デモの探索用（フロントのデモボタンと一致）。
resource "aws_ssm_parameter" "admin_password" {
  name   = "/${var.project_name}/bootstrap_admin_password"
  type   = "SecureString"
  key_id = aws_kms_key.main.id
  value  = var.demo_admin_password
}

# インスタンスロールへ「設定パラメータ読取・DB秘密読取・CMK復号」だけを最小権限で付与。
data "aws_iam_policy_document" "app_secrets" {
  statement {
    sid       = "ReadAppParameters"
    effect    = "Allow"
    actions   = ["ssm:GetParameter", "ssm:GetParameters", "ssm:GetParametersByPath"]
    resources = ["arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter/${var.project_name}/*"]
  }

  statement {
    sid       = "ReadDbMasterSecret"
    effect    = "Allow"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_db_instance.main.master_user_secret[0].secret_arn]
  }

  statement {
    sid       = "DecryptWithProjectCmk"
    effect    = "Allow"
    actions   = ["kms:Decrypt"]
    resources = [aws_kms_key.main.arn]
  }
}

resource "aws_iam_role_policy" "app_secrets" {
  name   = "${var.project_name}-app-secrets"
  role   = aws_iam_role.app.id
  policy = data.aws_iam_policy_document.app_secrets.json
}
