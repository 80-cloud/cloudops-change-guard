#!/bin/bash
# =====================================================================
# deploy-remote.sh — インスタンス側で実行される配備スクリプト
# CD の deploy ワークフローが SSM Run Command で base64 経由で送り込み、
# 成果物バケットから jar/dist を取り出して差し替え、サービスを再起動する。
# 引数: <artifacts-bucket> <sha> <project-name>
# =====================================================================
set -euxo pipefail
BUCKET="$1"
SHA="$2"
PROJECT="$3"

aws s3 cp "s3://${BUCKET}/${SHA}/artifact.tgz" /tmp/artifact.tgz
rm -rf /tmp/artifact
mkdir -p /tmp/artifact
tar -C /tmp/artifact -xzf /tmp/artifact.tgz

# jar を差し替え（所有は実行ユーザー、systemd が起動）
install -o cloudops -g cloudops -m 0644 /tmp/artifact/app.jar "/opt/${PROJECT}/app.jar"

# 静的ファイルを差し替え
rm -rf "/var/www/${PROJECT:?}"/*
cp -r /tmp/artifact/dist/* "/var/www/${PROJECT}/"
chown -R cloudops:cloudops "/var/www/${PROJECT}"

# サービス再起動（Flyway が起動時に migrate、DataSeeder が初期データを冪等投入）
systemctl restart "${PROJECT}.service"
systemctl reload nginx

echo "deployed ${SHA}"
