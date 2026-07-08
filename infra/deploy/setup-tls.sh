#!/bin/bash
# =====================================================================
# setup-tls.sh — TLS を有効化する（インスタンス側で SSM 経由 or 手動実行）。
#   自己署名（即時疎通・ブラウザ警告あり）:
#     setup-tls.sh <project> self-signed <ip-or-host>
#   Let's Encrypt（正規・DuckDNS 等の FQDN が EIP を指している前提）:
#     setup-tls.sh <project> letsencrypt <domain> <email> [--staging]
# 平文 conf を TLS 版へ差し替え、nginx -t で検証してから reload する。
# =====================================================================
set -euxo pipefail
PROJECT="${1:?usage: setup-tls.sh <project> <mode> ...}"
MODE="${2:?mode: self-signed | letsencrypt}"
SRC="$(cd "$(dirname "$0")" && pwd)"
TLS_DIR="/etc/${PROJECT}/tls"
install -d -m 0755 "$TLS_DIR"

case "$MODE" in
  self-signed)
    HOST="${3:-localhost}"
    openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
      -keyout "$TLS_DIR/privkey.pem" -out "$TLS_DIR/fullchain.pem" \
      -subj "/CN=${HOST}"
    DOMAIN="$HOST"
    ;;
  letsencrypt)
    DOMAIN="${3:?domain required}"
    EMAIL="${4:?email required}"
    EXTRA=()
    [ "${5:-}" = "--staging" ] && EXTRA=(--staging)
    dnf install -y certbot cronie
    systemctl enable --now crond
    # nginx が 80 で webroot を配信している前提で http-01 を通す
    certbot certonly --webroot -w "/var/www/${PROJECT}" -d "$DOMAIN" \
      --email "$EMAIL" --agree-tos --non-interactive "${EXTRA[@]}"
    ln -sf "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem" "$TLS_DIR/fullchain.pem"
    ln -sf "/etc/letsencrypt/live/${DOMAIN}/privkey.pem" "$TLS_DIR/privkey.pem"
    # 自動更新（更新後に nginx を reload）
    printf '0 0,12 * * * root certbot renew --quiet --deploy-hook "systemctl reload nginx"\n' \
      > /etc/cron.d/certbot-renew
    ;;
  *)
    echo "usage: setup-tls.sh <project> self-signed <host> | letsencrypt <domain> <email> [--staging]" >&2
    exit 2
    ;;
esac

# TLS 版 conf を設置（平文 conf を置き換え）→ 構文検証 → reload
sed -e "s/@PROJECT@/${PROJECT}/g" -e "s/@DOMAIN@/${DOMAIN}/g" \
  "$SRC/nginx-cloudops-tls.conf" > "/etc/nginx/conf.d/${PROJECT}.conf"
nginx -t
systemctl reload nginx

echo "TLS enabled (${MODE}) for ${DOMAIN}"
echo "次: SSM param cookie_secure=true / cors_allowed_origin=https://${DOMAIN} を反映し app を再起動（tfvars 更新→apply か、app.env 直更新→restart）"
