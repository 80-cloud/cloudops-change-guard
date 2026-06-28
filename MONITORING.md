# 無料枠 監視盤（全サービス横断）

複数アプリで使う無料枠サービスの **残枠・期限・課金リスク** を1ページで管理する。
今回（2026-06-01）の「GitHub Pages 404 事故」の再発防止が目的。

> 事故記録：[task-board/docs/incidents/cases/2026-06-01-github-pages-private-free-tier.md]
> 関連：[GitHub運用方針.md](./GitHub運用方針.md)・[docs/インフラ選択ガイド.md](./docs/インフラ選択ガイド.md)

---

## 🔴 今回の事故の教訓（最重要）

**「AWS だけが課金リスク」という思い込みが盲点を生んだ。**
GitHub 自体（Pages・Actions）も無料枠ベースのサービスで、同じ監視対象。

| 盲点だったもの | 制約 |
|---|---|
| **GitHub Pages** | **public リポ限定**（Free プラン）。private 化で即 404 |
| **GitHub Actions** | private repo は月 2,000 分。枯渇で全 CI 停止 + Pages デプロイも止まる |

→ **GitHub Pages を使うアプリは public 必須**。これを破ると数日アプリが落ちる。

---

## 📊 監視対象サービス一覧（テンプレ：アプリごとに記入）

| サービス | 制約 | 監視頻度 | 突然停止リスク | 確認先 |
|---|---|---|---|---|
| **GitHub Actions** | 2,000分/月（private）/ public 無制限 | 🔴 月初+月中 | ✅ 高 | [Billing](https://github.com/settings/billing) |
| **GitHub Pages** | public 限定・帯域~100GB/月 | 🟡 月初 | ✅ 高（private化で即停止）| 各リポ Settings → Pages |
| **AWS（EC2/RDS等）** | 12ヶ月限定無料枠（**認知済・カレンダー管理**）| 🟡 月初 | △（期限は把握済）| [Cost Management](https://console.aws.amazon.com/cost-management) |
| **Supabase** | プロジェクト 2個 / DB 500MB | 🟡 新規作成前 | △（既存は動く）| [Dashboard](https://app.supabase.com) |
| **Cloudflare Pages** | 500ビルド/月・配信無制限 | 🟢 四半期 | × 低 | CF Dashboard |
| **Cloudflare R2** | 10GB・egress 無制限 | 🟢 四半期 | × 低 | CF Dashboard |

---

## ✅ 月初 5分ルーティン（毎月1日）

### GitHub（最優先・今回の事故源）

```bash
# 1. 使用量が $0 か確認
#    → https://github.com/settings/billing で metered usage を見る

# 2. 公開アプリの URL が生きているか（Pages 使用アプリ分）
curl -s -o /dev/null -w "%{http_code}\n" https://<owner>.github.io/<repo>/

# 3. 公開すべきリポが public のままか確認
gh repo list <owner> --json name,visibility --jq '.[] | select(.visibility=="PRIVATE") | .name'
#    → Pages を使うリポがここに出たら 🔴 即 public に戻す
```

### AWS（Tier 4 アプリがある場合・期限は認知済）

- 各アプリの Budgets アラートが届いていないか
- 未使用 EC2 の停止 / 不要リソースの destroy

### Supabase（Tier 1 アプリがある場合）

- プロジェクト数 `N/2` を確認（新規作成の余地があるか）

---

## 🚨 アラート設定状況（アプリ横断・記入する）

| アプリ | サービス | Budget/上限 | アラート | 設定済? |
|---|---|---|---|---|
| （例）my-app | GitHub Actions | public（無制限）| 不要 | ✅ |
| | AWS | $X/月 | X段通知 | |
| | Supabase | プロジェクト 1/2 | 手動確認 | |

---

## 📋 アプリ別 使用サービス台帳（記入する）

新規アプリを立ち上げるたび、ここに「どのサービスを使うか」を1行追加する。
これがあれば「気づいたら無料枠を使い切っていた」を防げる。

| アプリ | Tier | ホスティング | DB | ストレージ | public? | 立ち上げ日 |
|---|---|---|---|---|---|---|
| （例）term-board | 0 | GitHub Pages | localStorage | — | ✅ public | 2026-05 |

---

## 🎯 設計原則（無料枠を枯らさないために）

1. **GitHub Pages を使うアプリは必ず public**（private 化禁止）
2. **CI は public repo で動かす**（Actions 無制限になる）
3. **private repo を多用するなら docs-only PR を `paths-ignore`**（CI 節約）
4. **Supabase は 2個まで、3個目以降は Cloudflare / Oracle に振る**
5. **AWS（Tier 4）は意図的選択のみ・常時1-2個まで・Budgets 必須**
6. **「期限/上限で突然止まる」サービスは全て月初チェックの対象**
