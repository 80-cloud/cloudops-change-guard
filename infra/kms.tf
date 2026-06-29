resource "aws_kms_key" "main" {
  description             = "${var.project_name} 共通暗号鍵"
  enable_key_rotation     = true
  deletion_window_in_days = 30
}

resource "aws_kms_alias" "main" {
  name          = "alias/${var.project_name}"
  target_key_id = aws_kms_key.main.key_id
}
