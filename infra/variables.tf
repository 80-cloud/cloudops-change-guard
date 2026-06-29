variable "region" {
  description = "デプロイ先 AWS リージョン"
  type        = string
  default     = "ap-northeast-1"
}

variable "project_name" {
  description = "リソース名・タグの接頭辞"
  type        = string
  default     = "cloudops-change-guard"
}

variable "environment" {
  description = "環境名（dev / staging / production）"
  type        = string
  default     = "dev"
}

variable "monthly_budget_usd" {
  description = "月次予算アラートのしきい値（USD）"
  type        = number
  default     = 5
}

variable "budget_alert_emails" {
  description = "予算アラートの通知先メールアドレス"
  type        = list(string)
  default     = []
}

variable "vpc_cidr" {
  description = "VPC の CIDR ブロック"
  type        = string
  default     = "10.0.0.0/16"
}

variable "instance_type" {
  description = "アプリ用 EC2 インスタンスタイプ（無料枠を既定）"
  type        = string
  default     = "t3.micro"
}

variable "db_instance_class" {
  description = "RDS インスタンスクラス（無料枠を既定）"
  type        = string
  default     = "db.t3.micro"
}

variable "db_engine_version" {
  description = "PostgreSQL のメジャーバージョン"
  type        = string
  default     = "16"
}

variable "db_name" {
  description = "作成するデータベース名"
  type        = string
  default     = "cloudops"
}

variable "db_username" {
  description = "マスターユーザー名（パスワードは AWS 管理）"
  type        = string
  default     = "cloudops"
}

variable "db_allocated_storage" {
  description = "RDS ストレージ容量（GB）"
  type        = number
  default     = 20
}
