#!/bin/bash
# =====================================================================
# run.sh — @PROJECT@ 起動ラッパー
# 起動のたびに Secrets Manager の現行 DB パスワードを取得して DB_PASSWORD を
# 上書きしてからアプリを exec する。RDS のマネージドパスワードは定期的に
# 自動更新されるため、初回起動時に app.env へ焼き込んだ値は時間が経つと古くなる。
# 起動時に最新値へ揃えることで、長期停止からの復帰後などに認証が合わず
# 起動できなくなるのを防ぐ。
# 取得に失敗した場合は app.env の既存値へフォールバックする
# （一時的な API 障害でサービスを落とさないため）。
# =====================================================================
set -euo pipefail

PROJECT="@PROJECT@"
export PATH="/usr/local/bin:${PATH}"
export AWS_DEFAULT_REGION="${AWS_REGION:?AWS_REGION required (app.env で設定)}"

if arn="$(aws ssm get-parameter --name "/${PROJECT}/db_secret_arn" --with-decryption --query 'Parameter.Value' --output text 2>/dev/null)" \
  && pw="$(aws secretsmanager get-secret-value --secret-id "${arn}" --query 'SecretString' --output text 2>/dev/null | python3 -c 'import sys,json;print(json.load(sys.stdin)["password"])' 2>/dev/null)" \
  && [ -n "${pw}" ]; then
  export DB_PASSWORD="${pw}"
else
  echo "run.sh: 警告: DB パスワードの再取得に失敗。app.env の既存値で起動します。" >&2
fi

exec /usr/bin/java -Xms256m -Xmx512m -jar "/opt/${PROJECT}/app.jar"
