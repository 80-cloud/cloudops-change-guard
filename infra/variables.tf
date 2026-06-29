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
