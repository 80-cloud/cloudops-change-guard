# Tier 移行ログ

このアプリのインフラ Tier 変更を時系列で記録する。
> 関連：[インフラ選択ガイド.md](./インフラ選択ガイド.md)・[../GitHub運用方針.md](../GitHub運用方針.md)

---

## 2026-06-28: 初期 Tier 確定

- **採用 Tier（開発/MVP）**: **ローカル Docker Compose**（DB+backend+frontend を一括起動。Tier 4 相当のサーバー型スタックをローカルで完結）
- **本番 Tier（候補・Phase 6 で確定）**: **Tier 4（AWS）** を候補として両論併記。面接で IaC/本番デプロイ経験を語る価値があるが、無料枠期限・運用コストに注意（AWS を既定にせず Phase 6 で採用判断）。
- **選定理由**: 本アプリは Java/Spring Boot + Postgres + JWT/RBAC のサーバー型が要件指定。AWS連携はモック/抽象化で開始するため、MVP はローカル Docker Compose で $0。AWS への展開は「面接ポートフォリオ価値 vs コスト」を Phase 6 で比較してから。
- **ポート**: backend 8084 / frontend 5177 / PostgreSQL 5436（既存アプリと連番分離）。
- **適用した防御**:
  - [x] CLAUDE.md にプロジェクト概要・Tier・選定理由を記録（実装セッション前に要記入＝[開発前チェックと既存アプリの教訓.md] 参照）
  - [ ] Phase 6 で AWS 採用判断時：Budgets 設定・`prevent_destroy`/deletion protection・PR ベース apply を着手前に確認（Tier 4 必須対策）
  - [ ] 版数整合：Java 25 / Gradle 9.1 / Spring Boot 3.5.x を他アプリと統一（build/runtime 一致＝term-board の教訓）

---

## 2026-06-30: AWS Tier 4 デプロイ雛形を追加（実 apply なし）

- **変更内容**: `infra/` に Terraform でデプロイ構成（VPC / プライベートサブネット / VPC フローログ / KMS 鍵 / RDS PostgreSQL / EC2 / IAM ロール / 予算アラート）を追加。実際の apply は行わない雛形。
- **適用した防御**:
  - [x] AWS Budgets を定義（通知先は tfvars）
  - [x] 重要リソース（RDS）に prevent_destroy と deletion_protection
  - [x] マスターパスワードは AWS 管理（コードに秘密を置かない）／ストレージ・Performance Insights・マスター秘密を KMS 鍵で暗号化
  - [x] EC2 は IMDSv2 必須・受信なし・SSM 運用（鍵レス）
  - [x] PR ベースのワークフロー（apply / destroy は人の承認をはさむ。AI 単独実行はしない）
  - [x] CLAUDE.md のプロジェクト概要に Tier 4 を記録
- **追加した GitHub workflow**: `.github/workflows/infra-check.yml`（認証情報なしで fmt / validate / tflint / tfsec。apply なし・infra 変更時のみ）
- **無料枠への影響**: 雛形のみで実リソースは未作成のため現状ゼロ。実 apply する場合は EC2 t3.micro / RDS db.t3.micro（12 か月無料枠）を既定にし Budgets で監視。
- **次に注意すべきこと**: 次フェーズ（実 AWS 統合）は実 plan 取込・CloudWatch 実値・実 apply の連携。実リソースを読む段はローカルエミュレート（Testcontainers）か実アカウントが要る。実 apply は人の承認を必ずはさむ。

---

## 移行記録テンプレ（コピーして使う）

```markdown
## YYYY-MM-DD: Tier X → Tier Y

- **変更理由**: （何の要件 / 制約で変わったか）
- **適用した防御**:
  - [ ] CLAUDE.md の Tier フィールド更新
  - [ ] 新 Tier の必須対策を [インフラ選択ガイド.md](./インフラ選択ガイド.md) §4 から漏れなく適用
  - [ ] README の技術スタック更新
  - [ ] 旧 Tier 固有のリソースを撤収（例：Supabase プロジェクト削除）
- **追加した GitHub workflow**: （ある場合）
- **削除した GitHub workflow**: （ある場合）
- **無料枠への影響**: （新 Tier の上限と現状の見積もり）
- **次に注意すべきこと**: （新 Tier 特有のリスク）
```

---

## Tier 変更がよく起こるパターン

機能追加 PR を書くとき、以下に該当しないか確認する：

| 起きがちな変化 | トリガー要件 |
|---|---|
| Tier 0 → Tier 1 | 他人とデータ共有・認可が必要に |
| Tier 1 → Tier 2 | Supabase プロジェクト 2/2 満杯 |
| Tier 0/1 → Tier 2/3 | リアルタイム通信を入れたい |
| Tier 0 → Tier 1（R2 併用） | ファイル保存・大容量 |
| Tier 1/2 → Tier 3/4 | サーバーで定期処理を回したい |
| Tier 1-3 → Tier 4 | 業務利用・SLA が要る |

→ 該当したら、PR をマージする前にこのログに記録 → Tier 別防御を適用 → CLAUDE.md を更新。
