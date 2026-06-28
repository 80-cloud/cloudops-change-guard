#!/usr/bin/env bash
# 公開ドキュメントの用語・命名チェック。
# 検査語そのものはこのスクリプトに書かない（公開リポに語一覧を残さない）。
#   → gitignore 済み scripts/blackbox-words.local.txt から読む。cp -R で運ばれ commit はされない。
set -eo pipefail
MODE="${1:-all}"

# 最優先ガード：非公開ファイル(*.local.*)が万一トラッキングされていたら即停止
# quotepath=false 必須：日本語名が "..." でエスケープされ grep に漏れるのを防ぐ
TRACKED_LOCAL=$(git -c core.quotepath=false ls-files | grep -E '\.local\.(md|txt)$' || true)
if [ -n "$TRACKED_LOCAL" ]; then
  echo "✘ 非公開ファイル(*.local.*) が追跡対象です:"
  echo "$TRACKED_LOCAL" | sed 's/^/    /'
  echo "  → .gitignore を確認し、git rm --cached <file> で追跡解除してください。"
  exit 1
fi

BANNED=()
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_WORDS="${BLACKBOX_WORDS_FILE:-$SCRIPT_DIR/blackbox-words.local.txt}"
if [ -f "$LOCAL_WORDS" ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%%#*}"; word="${line%%=>*}"
    word="$(printf '%s' "$word" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
    [ -n "$word" ] && BANNED[${#BANNED[@]}]="$word"
  done < "$LOCAL_WORDS"
fi

if [ "$MODE" = "--staged" ]; then
  FILES=$(git -c core.quotepath=false diff --cached --name-only --diff-filter=ACM | grep -E '\.(md|txt|yml|yaml|json|js|ts|tsx|jsx|py|java|gradle|sh|html|css)$' || true)
else
  FILES=$(git -c core.quotepath=false ls-files | grep -E '\.(md|txt|yml|yaml|json|js|ts|tsx|jsx|py|java|gradle|sh|html|css)$' || true)
fi

if [ ${#BANNED[@]} -eq 0 ]; then
  echo "[check] 語ファイル未配置のため語スキャンはスキップ（*.local.* ガードは通過）。"
  exit 0
fi
if [ -z "$FILES" ]; then
  echo "[check] 検査対象ファイルなし — OK"; exit 0
fi

FOUND=0
for kw in "${BANNED[@]}"; do
  while IFS= read -r f; do
    [ -z "$f" ] && continue; [ ! -f "$f" ] && continue
    if grep -Fn "$kw" "$f" >/dev/null 2>&1; then
      echo "✘ 不許可語「${kw}」を検出: ${f}"
      grep -Fn "$kw" "$f" | head -3 | sed 's/^/    /'
      FOUND=1
    fi
  done <<< "$FILES"
done
if [ $FOUND -ne 0 ]; then
  echo ""
  echo "==================================================================="
  echo " 用語・命名規律に違反 — コミット/プッシュをブロックしました"
  echo "==================================================================="
  exit 1
fi
echo "[check] 不許可語の残存なし — OK"
exit 0
