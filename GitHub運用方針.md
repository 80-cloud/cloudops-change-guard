# GitHub 運用方針（無料枠最大活用・ポートフォリオ最適化）

月50アプリ開発を **GitHub 無料枠のまま** 持続するための運用方針。
学習目的・ポートフォリオ・学習記録の3観点を同時に満たす設計。

> 関連：[NEW_REPO_CHECKLIST.md](./NEW_REPO_CHECKLIST.md)（リポ立ち上げ手順）・[MONITORING.md](./MONITORING.md)（無料枠監視盤）

---

## ⚠️ 過去事故からの最重要警告（2026-06-01）

**GitHub Pages を使うアプリを private 化すると、即座に 404 になる。**

GitHub Free プランは **private repo の Pages をサポートしない**。過去、公開アプリのリポが
private 化された結果、数日間アプリが 404 になる事故が発生した（[事故記録](#)）。

| 絶対ルール | 理由 |
|---|---|
| 🔴 **Pages を使うアプリは public 必須** | private 化で即停止 |
| 🔴 **「公開を止めたい」≠「private 化」** | コードを隠したいなら Cloudflare Pages 等へ移行する |
| 🟡 **private repo の Actions は月2,000分上限** | 枯渇すると Pages デプロイも CI も全停止 |

→ 詳細・復旧手順は [MONITORING.md](./MONITORING.md) と事故記録を参照。

---

## 🎯 大原則

### 1. **新規アプリは原則 public**

| 種別 | Actions | Pages | コスト |
|---|---|---|---|
| **public repo** | **無制限** | 無料 | **$0** |
| private repo | 月 2,000 分 | Free 不可 | 超過リスク |

→ public にする限り、無料枠は構造的に枯渇しない。

### 2. **ポートフォリオは「動くデモ＋公開コード」が最強**

採用担当が見られないコードは存在しないも同然。**未経験者の最大の武器は、コードを書いた証拠が公開されていること**。private 化は学習者・転職活動者にとって自分の武器を捨てる行為。

### 3. **学習記録はリポ自体が担う**

コミット履歴・PR 本文・docs/ がそのまま「何を試したか・何が分かったか」の記録になる。意識して書き残せば、後から自分の成長を振り返れる。

---

## ✅ 新規アプリ作成時のチェック（Phase 6 補強）

[NEW_REPO_CHECKLIST.md](./NEW_REPO_CHECKLIST.md) の Phase 6 で `gh repo create --public` を実行することを **絶対に守る**。`--private` を使うのは以下の例外のみ：

- 商用前で競合保護が必要（学習用は該当しない）
- 秘密の試作で公開前提でない
- 顧客機密データを含む

→ **学習・ポートフォリオ目的では `--public` 一択**。

```bash
# 標準コマンド（必ず --public）
gh repo create <owner>/<repo> --public --source . --push
```

---

## 🔧 CI Workflow の軽量化テンプレ

`.github/workflows/ci.yml` の標準形（テンプレに含める）：

```yaml
name: CI

on:
  pull_request:
    branches: [main]
    paths-ignore:
      - '**.md'
      - 'docs/**'
      - '.gitignore'
      - 'LICENSE'

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  test:
    runs-on: ubuntu-latest
    timeout-minutes: 10
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 24
          cache: 'npm'
      - run: npm ci
      - run: npm test
```

**重要ポイント**：

- `paths-ignore` で docs-only PR をスキップ（無駄実行を回避）
- `concurrency: cancel-in-progress: true` で同ブランチの古い run を即キャンセル
- `timeout-minutes: 10` で暴走防止
- `cache: 'npm'` で依存解決を高速化

---

## 🤖 Claude Code Review の方針

**デフォルト：無効**（必要なリポだけ追加）。

理由：
- 失敗時もミニッツ消費
- public repo なら無料だが、起動回数が増えるとレートリミットに影響
- 個人開発では人によるレビュー文化との両立が難しい

**追加する場合の最小設定**：

```yaml
# .github/workflows/claude-code-review.yml
on:
  pull_request:
    types: [opened]   # synchronize（再push）には反応しない
    paths-ignore:
      - '**.md'
      - 'docs/**'

concurrency:
  group: claude-review-${{ github.event.pull_request.number }}
  cancel-in-progress: true
```

→ PR 作成時の初回だけレビュー、push のたびには走らせない。

---

## 💰 課金防止の Spending Limit 設定（一度きり）

[Budgets and alerts](https://github.com/settings/billing/budgets) で以下を設定（既にあれば確認のみ）：

| 製品 | Budget | Stop usage |
|---|---|---|
| Actions | $0 | **Yes** |
| Codespaces | $0 | **Yes** |
| Packages | $0 | **Yes** |
| Git LFS | $0 | **Yes** |
| AI Credit SKUs | $0 | **Yes** |

→ 万一 private repo で予想外の課金が発生しても、自動停止して被害ゼロ。

---

## 🏗 デプロイ先別の GitHub 側考慮

デプロイ先（インフラ Tier）の選び方は [docs/インフラ選択ガイド.md](./docs/インフラ選択ガイド.md) を参照。
ここでは **GitHub Actions / リポジトリ運用の側面** だけ補足する。

| Tier | デプロイ先 | GitHub 側の考慮 | 公開推奨 |
|---|---|---|---|
| **0** | GitHub Pages（静的）| `term-board-pages.yml` 相当の自動デプロイ。約30秒/run | ✅ public 必須（Free では private Pages 不可）|
| **1** | Supabase + R2 | CI でビルドのみ。デプロイは Supabase 側 / R2 直接 push | ✅ public（API キーは Secrets で管理）|
| **2** | Cloudflare（Pages/Workers/D1）| `wrangler deploy` を Action から実行 | ✅ public（CF トークンは Secrets）|
| **3** | Oracle Always Free（VM）| SSH デプロイ workflow。鍵管理に注意 | ✅ public（SSH 秘密鍵は Secrets）|
| **4** | **AWS（有料）** | terraform plan / build / deploy の Actions。**重い・課金リスクあり** | ⚠️ **慎重に判断**（次節参照）|

### Tier 4（AWS）特有の GitHub 運用ルール

CLAUDE.md §9 と [docs/インフラ選択ガイド.md](./docs/インフラ選択ガイド.md) §4 を厳守。**GitHub 側で追加で必要な対策**：

#### 1. terraform plan を `paths` で絞る（必須）

```yaml
on:
  pull_request:
    paths:
      - 'infra/**'
      - '.github/workflows/terraform-plan.yml'
```

→ アプリコード変更のたびに terraform plan を走らせない。AWS API への無駄なアクセスも防ぐ。

#### 2. terraform apply は **手動・人間承認**（CI で走らせない）

```yaml
on:
  workflow_dispatch:   # 手動のみ
    inputs:
      confirm:
        description: '"apply" と入力して確認'
        required: true
```

→ CLAUDE.md §6（AI 誤操作防止）に直結。`terraform apply -auto-approve` は CI で絶対に走らせない。

#### 3. Secrets の置き場所

| 種類 | 置き場所 |
|---|---|
| AWS Access Key | GitHub Secrets（Repository or Environment）|
| terraform.tfvars（パスワード等）| `.gitignore` に追加・Secrets で渡す |
| state ファイル | S3 + DynamoDB（リモート）or ローカル管理 |

→ コミット禁止語に `*.tfvars`・`*.pem`・`access_key` を含める（pre-commit hook で検知）。

#### 4. 公開リポでの AWS 構成露出

AWS（Tier 4）アプリのリポを public にする場合：

| 露出する情報 | リスク評価 |
|---|---|
| VPC/SG/IAM ロールの構成パターン | 🟡 中（手口は標準的なので大きな問題なし）|
| インスタンスタイプ・リージョン | 🟢 低 |
| 環境変数のキー名（値は Secrets）| 🟢 低 |
| **Account ID・実 ARN・実 IP** | 🔴 高（`pre-commit` で検知・除去）|

→ AWS の **構成パターンは公開して学習資産にする**、**実 ID/値は絶対にコミットしない**、を徹底する。

#### 5. AWS 撤収後の GitHub 側残務

```bash
# 1. terraform destroy（要人間承認）
cd infra && terraform destroy

# 2. 後始末
gh secret list                            # 不要 Secrets を削除
gh workflow disable terraform-plan.yml   # 撤収後は workflow も停止
```

→ AWS 撤収後も GitHub Actions が動き続けると無駄にミニッツを消費するので止める。

### Tier 別の月次 GitHub 消費目安（参考）

| Tier | 月次 Actions 消費 | 課金リスク |
|---|---|---|
| Tier 0（Pages）| ~5-10 分 | なし（public 無制限）|
| Tier 1-2 | ~10-30 分 | なし（public 無制限）|
| Tier 3 | ~20-40 分 | なし（public 無制限）|
| **Tier 4（AWS）** | **~50-150 分**（terraform plan・build・smoke）| AWS 側は別途課金（Budgets で防御）|

→ public repo であれば全 Tier で GitHub 課金は発生しない。**Tier 4 のみ AWS 側の課金を別途管理**。

---

## 📊 月次運用ルーティン（5分）

毎月 1 日に以下を順にチェック：

### GitHub 側

1. [Billing Overview](https://github.com/settings/billing) で **Current metered usage** が `$0` のまま
2. 違う場合：[Usage](https://github.com/settings/billing/usage) で消費元を特定 → 該当 workflow を `paths-ignore` で絞る

### AWS 側（Tier 4 アプリがある場合のみ）

3. [AWS Billing Dashboard](https://console.aws.amazon.com/billing/home) で **当月の予測請求額**を確認
4. **Budgets** のアラートが届いていないか確認
5. **未使用の EC2 インスタンスを停止 or terraform destroy**
6. **CloudWatch Logs / RDS snapshot の自動増加**をチェック（無料枠超過の典型）

→ これだけで「気づいたら使い切ってた」「気づいたら AWS で課金されてた」両方を防げる。

---

## 📁 ポートフォリオ最適化（リポ単位）

各リポの README に以下を必ず含める：

| 項目 | 例 |
|---|---|
| **1行サマリ** | 「IT 用語の学習＋面接対策アプリ」など |
| **動くデモ URL** | Pages / Cloudflare Pages 等 |
| **対象ユーザー** | 「IT スクール生・独学者」など |
| **技術スタック** | React 19 + TypeScript + Vite |
| **主な機能** | 表で簡潔に（5-8項目）|
| **スクリーンショット or GIF** | 動作イメージを視覚で |
| **ローカル開発手順** | `<details>` で折り畳み可 |

→ 採用担当が **3分で「何ができるアプリか」を理解できる**構造が目標。

---

## 📝 学習記録の残し方

学習目的・将来振り返り用に、以下を意識：

### コミットメッセージ

- Conventional Commits（`feat:` `fix:` `chore:` `docs:` 等）
- 「なぜ」を本文に書く（What はコードで分かる、Why は本文だけ）

```
feat: ダッシュボードにカバレッジセクション追加

学習者が「全248語中何件触れたか」を一目で把握できるよう、
進捗の可視化を進めた（学習継続性向上が目的）。
```

### PR 本文

学習意図・試行錯誤を本文に残す：

```markdown
## 何を試したか
- React Context vs Zustand を比較
- 状態管理ライブラリの選定理由を docs に記録

## 何が分かったか
- 小規模なら Context で十分
- 中規模以降は Zustand が見通し良い

## 次の課題
- パフォーマンス計測で実際の差を確認したい
```

### docs/ への蓄積

- `docs/技術選定.md`：なぜこの技術を選んだか
- `docs/インシデント記録.md`：詰まった点と解決方法
- `docs/設計判断.md`：トレードオフ判断の記録

→ これらは **学習資産** であり、**面接で「学んだことを言語化できる」証拠** にもなる。

---

## 🚫 やらないこと（重要）

### GitHub 全般

| やらないこと | 理由 |
|---|---|
| ❌ 新規アプリを private で作る | 公開していなければポートフォリオにならない |
| ❌ 失敗中の workflow を放置 | 失敗でもミニッツ消費 |
| ❌ Spending limit を上げる | 不意の課金リスク |
| ❌ Claude Code Review を全 PR に走らせる | 学習用途では過剰 |
| ❌ GitHub Pro 課金 | 学習者・転職活動者には ROI 低 |
| ❌ docs を別リポに分離 | 学習記録はアプリ本体と紐付いてこそ価値 |

### Tier 4（AWS）特有

| やらないこと | 理由 |
|---|---|
| ❌ `terraform apply -auto-approve` を CI で実行 | 本番リソース消失事故の典型（CLAUDE.md §9）|
| ❌ `terraform destroy` を AI 単独で実行 | RDS バックアップごと消える事故あり |
| ❌ `prevent_destroy = true` を外したまま放置 | 防御線を解除した状態で作業しない |
| ❌ AWS Access Key を `.tfvars` にコミット | pre-commit hook で検知・即無効化 |
| ❌ AWS 撤収忘れ | 学習終了時は必ず `terraform destroy`・確認後にリポ workflow も停止 |
| ❌ AWS Budgets を未設定で着手 | 課金上限がないと事故時の被害が無限 |

---

## 🎯 50アプリ達成の現実的なペース

- 1日 1-2 アプリ立ち上げ × 25-30日 = 25-60 アプリ/月
- 全部 public なので **GitHub 無料枠消費 $0**
- 完成度は問わない（学習プロセス自体がポートフォリオ）

→ 「完成させる」より「**始めて履歴を残す**」を優先するのが現実的。

### 50アプリの Tier 内訳（個数の目安）

| Tier | 立ち上げ時の個数 | 理由 |
|---|---|---|
| Tier 0（Pages 静的）| **30〜35個** | 立ち上げが最速・無料・公開も即時 |
| Tier 1-2（Supabase / CF）| **10〜15個** | 多人数アプリの実装経験 |
| Tier 3（Oracle）| **2〜3個** | サーバー運用を学びたいとき |
| **Tier 4（AWS）** | **1〜3個まで** | **意図的な選択のみ**。常時1-2個に留める |
| **合計** | **約50個** | |

→ AWS を含めても総コストは月数百円〜数千円（Free Tier 内なら $0）。Tier 4 が増えると課金リスクが指数関数的に上がるので **意識的に絞る**。

---

## 🔄 Tier 変更追跡（重要：通知の代替機能）

**Tier 0 で始めたアプリが、機能追加で Tier 1/2/3 に昇格するケースが多発する**。例：

- 「個人ツール（Tier 0）」→ 友人と共有したくなって「多人数（Tier 1）」へ
- 「Supabase（Tier 1）」→ プロジェクト数 2/2 上限に当たって「Cloudflare（Tier 2）」へ
- 「Cloudflare（Tier 2）」→ 「自前サーバーで握りたくなって Oracle（Tier 3）」へ

Tier が変わると **適用すべき防御線・無料枠の上限・GitHub 側 workflow** がすべて変わる。**通知し忘れると課金事故 or 機能喪失** に繋がる。

### Tier 変更時の必須プロセス（自己通知の代替）

1. **CLAUDE.md の Tier フィールドを更新**

   ```markdown
   | 項目 | 内容 |
   |---|---|
   | インフラ Tier | **Tier 1**（旧：Tier 0、2026-06-15 移行）|
   | 選定理由 | 友人と共有するため認可が必要に。Supabase 採用 |
   ```

2. **`docs/Tier移行ログ.md` に追記**（テンプレに含める）

   ```markdown
   ## 2026-06-15: Tier 0 → Tier 1
   - 変更理由: 友人と共有機能が要件に追加
   - 適用した防御: Supabase RLS / API キーを Secrets 管理 / 認可テスト追加
   - 削除した workflow: なし（既存のまま）
   - 追加した workflow: なし（Supabase 側でデプロイ）
   ```

3. **新 Tier の防御を [docs/インフラ選択ガイド.md](./docs/インフラ選択ガイド.md) §4 から漏れなく適用**

4. **README の技術スタックも更新**（外部から見て古い情報が残らないように）

### Tier 変更を見落とさないチェックポイント

以下の変更時に「**Tier が変わっていないか**」を自問する：

| トリガー | 起こりがちな Tier 変化 |
|---|---|
| 「他人とデータ共有したい」追加要件 | Tier 0 → Tier 1 |
| 「複数アプリで DB を共有したい」 | Tier 1 → Tier 2 |
| 「リアルタイム通信を入れたい」 | Tier 0/1 → Tier 2/3 |
| 「ファイル保存が要る・大容量」 | Tier 0 → Tier 1（R2 併用）|
| 「サーバーで定期処理を回したい」 | Tier 1/2 → Tier 3/4 |
| 「業務利用・SLA が要る」 | Tier 1-3 → Tier 4 |

→ 機能追加 PR を書くとき、これらの要件が加わっていないか **必ず確認** する。

### Tier 変更を機械的に検知する（推奨：将来追加）

完全な通知機能は無理だが、以下で半自動化できる：

| 仕組み | 内容 |
|---|---|
| **CLAUDE.md の Tier 整合性チェック** | `scripts/tier-check.sh` でリポ内の使用技術と CLAUDE.md の Tier が整合しているか検証（テンプレに追加余地）|
| **依存パッケージで検知** | `package.json` に `@supabase/supabase-js` 追加 = Tier 1 移行のシグナル。pre-commit で警告 |
| **環境変数で検知** | `.env.example` に `AWS_*` キーが追加された = Tier 4 移行 |

→ これらは「**変更を見落とさないための仕組み**」であり、定期的にテンプレを進化させていく。


---

## 参考リンク

- [NEW_REPO_CHECKLIST.md](./NEW_REPO_CHECKLIST.md)：リポ立ち上げ手順
- [CLAUDE.md](./CLAUDE.md)：開発ルール
- [GitHub Free プラン](https://docs.github.com/en/get-started/learning-about-github/githubs-plans)
- [Pricing - Actions](https://github.com/pricing#pricing-and-plans-extras)
