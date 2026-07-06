#!/bin/bash
# =====================================================================
# provision.sh — EC2 の配線（runtime導入→設定設置→秘密取得→サービス有効化）
# user_data から PROJECT_NAME / AWS_REGION を受けて実行する。冪等。
# アプリの jar / 静的ファイルは CD が届けるまで存在しないため、その間は
# nginx がプレースホルダを配信し、app サービスは待機する。
# =====================================================================
set -euxo pipefail
: "${PROJECT_NAME:?PROJECT_NAME is required}"
: "${AWS_REGION:?AWS_REGION is required}"
export AWS_DEFAULT_REGION="${AWS_REGION}"
SRC="$(cd "$(dirname "$0")" && pwd)"

exec > >(tee -a /var/log/provision.log) 2>&1
echo "===== provision start: $(date -Iseconds) ====="

# --- 1. swap 2G（t3.micro=RAM 1GB。ビルドはCIで行い箱では jar 実行のみだが安全余裕）
if ! swapon --show | grep -q /swapfile; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >>/etc/fstab
  echo 'vm.swappiness=10' >/etc/sysctl.d/99-swap.conf
  sysctl -p /etc/sysctl.d/99-swap.conf
fi

# --- 2. runtime（JDK 25 / nginx）
dnf update -y
dnf install -y nginx unzip
rpm --import https://yum.corretto.aws/corretto.key
curl -fsSL https://yum.corretto.aws/corretto.repo -o /etc/yum.repos.d/corretto.repo
dnf install -y java-25-amazon-corretto-devel

# --- 3. AWS CLI v2（SSM/Secrets 取得に使用。SSM Agent は AL2023 標準）
if ! command -v aws >/dev/null 2>&1; then
  curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-$(uname -m).zip" -o /tmp/awscliv2.zip
  unzip -q -o /tmp/awscliv2.zip -d /tmp
  /tmp/aws/install --update
fi
systemctl enable --now amazon-ssm-agent || true

# --- 4. 実行ユーザーとディレクトリ
id -u cloudops >/dev/null 2>&1 || useradd -r -s /sbin/nologin cloudops
install -d -o cloudops -g cloudops "/opt/${PROJECT_NAME}" "/var/www/${PROJECT_NAME}"
install -d -o root -g cloudops -m 0750 "/etc/${PROJECT_NAME}"

# --- 5. nginx 設定（既定サーバを持たない最小 nginx.conf ＋ アプリ用 conf.d）
cat >/etc/nginx/nginx.conf <<'NGINXMAIN'
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log;
pid /run/nginx.pid;
events { worker_connections 1024; }
http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    keepalive_timeout 65;
    server_tokens off;
    include /etc/nginx/conf.d/*.conf;
}
NGINXMAIN
sed "s/@PROJECT@/${PROJECT_NAME}/g" "${SRC}/nginx-cloudops.conf" >"/etc/nginx/conf.d/${PROJECT_NAME}.conf"

# --- 6. systemd ユニット
sed "s/@PROJECT@/${PROJECT_NAME}/g" "${SRC}/cloudops-change-guard.service" >"/etc/systemd/system/${PROJECT_NAME}.service"

# --- 7. 設定・秘密を取得して EnvironmentFile を生成
get_param() { aws ssm get-parameter --name "/${PROJECT_NAME}/$1" --with-decryption --query 'Parameter.Value' --output text; }
DB_URL="$(get_param db_url)"
DB_USER="$(get_param db_user)"
DB_SECRET_ARN="$(get_param db_secret_arn)"
JWT_SECRET="$(get_param jwt_secret)"
CORS_ORIGIN="$(get_param cors_allowed_origin)"
COOKIE_SECURE="$(get_param cookie_secure)"
PROFILES="$(get_param spring_profiles_active)"
SEED="$(get_param seed_enabled)"
ADMIN_PW="$(get_param bootstrap_admin_password)"
DB_PASSWORD="$(aws secretsmanager get-secret-value --secret-id "${DB_SECRET_ARN}" --query 'SecretString' --output text | python3 -c 'import sys,json;print(json.load(sys.stdin)["password"])')"

umask 027
cat >"/etc/${PROJECT_NAME}/app.env" <<ENV
DB_URL=${DB_URL}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
JWT_COOKIE_SECURE=${COOKIE_SECURE}
CORS_ALLOWED_ORIGIN=${CORS_ORIGIN}
BOOTSTRAP_ADMIN_PASSWORD=${ADMIN_PW}
SEED_ENABLED=${SEED}
SPRING_PROFILES_ACTIVE=${PROFILES}
AWS_REGION=${AWS_REGION}
ENV
chown root:cloudops "/etc/${PROJECT_NAME}/app.env"
chmod 0640 "/etc/${PROJECT_NAME}/app.env"

# --- 8. プレースホルダ（CD が dist を届けるまでの一次表示）
if [ ! -f "/var/www/${PROJECT_NAME}/index.html" ]; then
  cat >"/var/www/${PROJECT_NAME}/index.html" <<'PLACEHOLDER'
<!doctype html><meta charset="utf-8"><title>CloudOps Change Guard</title>
<h1>CloudOps Change Guard</h1><p>Provisioned. Application artifact pending deployment.</p>
PLACEHOLDER
  chown -R cloudops:cloudops "/var/www/${PROJECT_NAME}"
fi

# --- 9. サービス有効化（jar 未配備なら app は起動失敗で待機。CD 後に稼働）
systemctl daemon-reload
systemctl enable nginx
systemctl restart nginx
systemctl enable "${PROJECT_NAME}.service"
systemctl restart "${PROJECT_NAME}.service" || echo "app jar 未配備のため待機（CD が配備後に起動する）"

echo "===== provision done: $(date -Iseconds) ====="
