# infra/ — AWS デプロイ構成（Terraform）

このディレクトリは CloudOps Change Guard を AWS 上で動かす場合の構成を Terraform で記述した雛形です。現時点では雛形であり、実際の terraform apply は行いません（コストと不可逆操作を伴うため）。

## 構成
- ネットワーク（VPC / プライベートサブネット / VPC フローログ）
- 共通 KMS 鍵（自動ローテーション有効）
- データベース（RDS PostgreSQL・prevent_destroy / deletion_protection）
- アプリ実行（EC2・SSM 運用・IMDSv2 必須・KMS 暗号化 EBS）
- 予算アラート（AWS Budgets）

## 方針
- terraform apply / destroy は人の承認を必ずはさむ。自動適用はしない。
- 重要リソース（データベース等）には prevent_destroy を付け、誤削除を防ぐ。
- 想定インスタンスは無料枠の範囲（EC2 t3.micro / RDS db.t3.micro）を既定にする。
- マスターパスワードは AWS 管理にし、秘密をコードに置かない。

## 静的チェック（認証情報なしで実行・apply はしない）
- terraform -chdir=infra fmt -recursive
- terraform -chdir=infra init -backend=false
- terraform -chdir=infra validate
- cd infra && tflint --init && tflint
- tfsec infra

同じ静的検査を CI（.github/workflows/infra-check.yml）でも実行する。

## 設定
terraform.tfvars.example をコピーして terraform.tfvars を作成し、通知先メール等を記入する（terraform.tfvars は追跡対象外）。
