# infra/ — AWS デプロイ構成（Terraform）

このディレクトリは CloudOps Change Guard を AWS 上で動かす場合の構成を Terraform で記述した雛形です。現時点では雛形であり、実際の terraform apply は行いません（コストと不可逆操作を伴うため）。

## 方針
- terraform apply / destroy は人の承認を必ずはさむ。自動適用はしない。
- 重要リソース（データベース等）には prevent_destroy を付け、誤削除を防ぐ。
- 想定インスタンスは無料枠の範囲（EC2 t3.micro / RDS db.t3.micro）を既定にする。
- コスト上限の超過に気づけるよう AWS Budgets を定義する（通知先は tfvars で設定）。

## 静的チェック（認証情報なしで実行・apply はしない）
- terraform -chdir=infra fmt -recursive
- terraform -chdir=infra init -backend=false
- terraform -chdir=infra validate
- cd infra && tflint --init && tflint
- tfsec infra

## 設定
terraform.tfvars.example をコピーして terraform.tfvars を作成し、通知先メール等を記入する（terraform.tfvars は追跡対象外）。
