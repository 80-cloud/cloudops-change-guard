# terraform plan の成果物（apply 前のレビュー対象）を保管するバケット。
# アプリは read-only でここから plan テキストを取得し、リスク判定の入力にする。
# apply 結果の証跡をたどる起点になるため、誤削除を防ぐ prevent_destroy を付ける。
resource "aws_s3_bucket" "plans" {
  bucket_prefix = "${var.project_name}-plans-"
  force_destroy = false

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_public_access_block" "plans" {
  bucket                  = aws_s3_bucket.plans.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "plans" {
  bucket = aws_s3_bucket.plans.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "plans" {
  bucket = aws_s3_bucket.plans.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.main.arn
    }
    bucket_key_enabled = true
  }
}

# アクセスログはフローログ用バケットへ集約する（プレフィックスで分離）。
resource "aws_s3_bucket_logging" "plans" {
  bucket        = aws_s3_bucket.plans.id
  target_bucket = aws_s3_bucket.flow_logs.id
  target_prefix = "s3-access/plans/"
}
