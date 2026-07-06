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

# --- アプリ配備（provision が参照）-----------------------------------------
variable "app_repo_url" {
  description = "起動時に clone する公開リポジトリ（配備設定の取得元・ビルドはしない）"
  type        = string
  default     = "https://github.com/80-cloud/cloudops-change-guard.git"
}

variable "app_repo_branch" {
  description = "配備設定を取得するブランチ"
  type        = string
  default     = "main"
}

variable "github_repo" {
  description = "CD を実行する GitHub リポジトリ（OIDC の sub 条件に使用）"
  type        = string
  default     = "80-cloud/cloudops-change-guard"
}

variable "public_origin" {
  description = "公開オリジン（CORS 用・同一オリジン配信のため主に保険。TLS 化後に https://<host> を設定）"
  type        = string
  default     = ""
}

variable "cookie_secure" {
  description = "JWT クッキーを Secure 属性にするか（TLS 化=Phase 3 までは false）"
  type        = bool
  default     = false
}

variable "spring_profiles_active" {
  description = "本番 Spring プロファイル（空=Mock / aws=実 read）"
  type        = string
  default     = ""
}

variable "seed_enabled" {
  description = "初回起動でデモ用初期データを投入するか"
  type        = bool
  default     = true
}

variable "demo_admin_password" {
  description = "デモ管理者のパスワード。公開デモの探索用でありフロントのデモボタンと一致させる（実データは投入しない）"
  type        = string
  default     = "ChangeMe!2026"
}
