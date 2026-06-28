# AWS・IaC連携方針（CloudOps Change Guard）

> この段階で AWS への本番接続は不要。ただし将来 CloudFormation / Terraform / 各種 AWS サービスへ**差し替えで**連携できる境界（Port/Adapter）を設計する。TerraformとCloudFormationは**中核ロジックに埋め込まず**、外部連携サービス＝Adapter として交換可能にする。

> **知見ベース**：AWS連携の本番前提・注意点は AWS図鑑（公開 `80-cloud/aws-zukan`）のサービス本番前提・横断ルールに基づく（出典＝AWS公式 / 一次運用知見）。リスク判定の根拠対応は [リスク判定ルール.md] §6。

## 1. 境界設計（Port / Adapter）

Controller / Service は **Port（インターフェース）** にのみ依存し、AWS SDK や IaC CLI を直接呼ばない。

```
                ┌──────────────── アプリ中核（Service） ────────────────┐
                │  リスク判定 / ポリシー / 状態機械 / 承認 / 監査          │
                └───────▲───────────▲────────────▲────────────▲─────────┘
        Port:           │           │            │            │
        IaCChangeProvider   MonitoringStatusProvider  InstanceStatusProvider
                │           │            │            │
   ┌────────────┴───┐  ┌────┴────┐  ┌────┴────┐  ┌────┴──────────┐
   │TerraformPlanAdapter│ │CloudFormation │ │CloudWatch │ │SystemsManager │   ← 本番Adapter（将来）
   │                │  │ChangeSetAdapter│ │Adapter   │ │StatusAdapter  │
   └────────────────┘  └─────────┘  └─────────┘  └───────────────┘
   ┌────────────────┐  ┌──────────────────────────────────────────┐
   │MockIaCChangeProvider│ │MockMonitoringStatusProvider / Mock各種  │   ← ローカル開発（既定）
   └────────────────┘  └──────────────────────────────────────────┘
```

### Port 一覧
| Port | 役割 | 本番 Adapter | ローカル |
|---|---|---|---|
| `IaCChangeProvider` | IaC差分の取り込み・正規化 | `TerraformPlanAdapter` / `CloudFormationChangeSetAdapter` | `MockIaCChangeProvider` |
| `MonitoringStatusProvider` | 監視状態の取得 | `CloudWatchMonitoringAdapter` | `MockMonitoringStatusProvider` |
| `InstanceStatusProvider` | EC2/SSM 状態の取得 | `SystemsManagerStatusAdapter` | `MockInstanceStatusProvider` |

- 切替は Spring プロファイル（`local` は Mock、`aws` は実 Adapter）。`@Profile` / 設定で DI。
- **ControllerやServiceからAWS SDKを直接呼び出さない**（テストで漏れを検出：SDK型がService層に現れないことを確認）。

## 2. IaC差分の正規化

将来連携対象の差分を、共通の正規化モデル `NormalizedChange{ resourceType, action, properties, environment, sourceText }` に変換する。`action ∈ {CREATE, UPDATE, DELETE, REPLACE}`。

| 入力 | パーサ | 抽出方法 |
|---|---|---|
| Terraform plan（テキスト） | `TerraformTextParser` | `# ... will be destroyed` / `-/+ ... replacement` 等の構文 |
| terraform plan -json（将来） | `TerraformJsonParser` | `resource_changes[].change.actions`（create/update/delete/replace）を直接マッピング |
| CloudFormation Change Set | `CloudFormationChangeSetParser` | `Action: Add/Modify/Remove/Replace`・ResourceType |

> 第1段階はテキスト解析でよいが、**文字列一致が散らばる実装にしない**。パーサ→正規化モデル→`RiskEngine` の段で責務分離（[リスク判定ルール.md]）。delete/replace を高リスクへ渡す。

## 3. 連携対象（将来）

CloudFormation Change Set / CloudFormation Drift Detection / Terraform plan / terraform show -json / CloudWatch / Systems Manager / AWS Health / AWS Config / CloudTrail。いずれも Port 経由で追加し、中核は無改修。

## 4. 採用判断：Terraform / CloudWatch / Ansible

| 技術 | 採用判断（Phase 1時点） | 理由 |
|---|---|---|
| **Terraform** | **採用候補（Phase 6で本決定）** | IaC運用の経験を面接で説明でき、構成を再現可能にできる。本アプリの題材とも一致。ただし**Webアプリ本体から直接 `terraform apply` はしない**（plan の安全確認・承認が主目的） |
| **CloudWatch** | **採用（必須候補）** | 「変更後の正常性確認」という本アプリの価値に直結。最小構成（ALB 5XX / UnHealthyHostCount / EC2 StatusCheckFailed / RDS FreeStorageSpace / アプリエラーログ / Alarm状態）から開始 |
| **Ansible** | **現時点は不採用（保留）** | MVP は OS内部設定の自動化要件が無い。Terraform（資源）＋Docker（実行環境）＋SSM（安全な運用操作）で充足。下記の採用条件に該当したら再検討 |

### Ansible 採用条件（満たせば再検討）
- EC2 への設定変更・アプリデプロイ後の確認を自動化したい
- SSM 経由で安全に Playbook を実行したい（SSH 直結は前提にしない）
- CloudFormation/Terraform では扱いづらい OS 内部設定（nginx・Javaランタイム・再起動等）を管理したい

→ 採用する場合も **SSM Run Command / Session Manager 経由**を前提にし、SSH 鍵の直接接続は避ける（aws-zukan service `ssm`：踏み台レス接続・最小権限・Session Manager のログ記録）。不採用の場合は「Terraform・Docker・SSM で十分」である理由を README に明記する。

### IaC適用の失敗時挙動（健全性確認・復旧設計の根拠）
- **CloudFormation**：既定ではスタック作成失敗時に自動ロールバックし、**成功していたリソースも削除**される（aws-zukan service `cloudformation`）。残したい場合は `--disable-rollback` / `on-failure DO_NOTHING` を明示。原因はスタックの **events** を見ないと分からない → 実施後ヘルスチェックに「適用結果＋events 参照」を残す設計の根拠。本番前提＝change set で差分確認・スタックポリシー/削除保護・最小権限の実行ロール。
- これは本アプリの「**IaC適用成功 ≠ サービス正常**」「失敗時にも復旧できる状態を残す」設計（[状態遷移設計.md] COMPLETE 条件・ROLLED_BACK）と直結する。

### 実施前チェックリストの根拠（aws-zukan `deploy-audit` ⇔ 標準チェック項目）
apply 前点検（横断ルール `deploy-audit`）を、本アプリの実施前チェック（[ER図.md] `pre_execution_checks.check_type`）へ機械化する：

| deploy-audit の点検 | 実施前チェック項目 |
|---|---|
| ①機密情報が平文でコミットされていないか grep | （リスク判定＋ SEC-9 で別途担保）／IMPACT |
| ②環境固有値（IP/エンドポイント/バケット名）の外部化 | IMPACT（影響範囲確認） |
| ③無料枠／予算の範囲 | WINDOW/IMPACT（コスト影響） |
| ④ビルド/ランタイムの版数突合 | IMPACT |
| ⑤plan 差分の意図しない削除/置換を**目視** | （リスク判定 TF_DESTROY_BULK 等＋ REVIEWER 承認） |
| ⑥重要リソースに削除保護（prevent_destroy / deletion protection） | BACKUP/ROLLBACK（復旧余地の確認） |

標準チェック＝BACKUP（バックアップ確認）/ROLLBACK（ロールバック手順確認）/MONITORING（監視設定確認）/IMPACT（影響範囲確認）/STAKEHOLDER（関係者連絡）/WINDOW（実施時間帯）/APPROVAL（承認完了確認）。production は必須（[状態遷移設計.md]）。

## 5. 実行の扱い（plan確認・承認が主目的）

Webアプリが本番 AWS へ直接変更を適用することは必須にしない。代わりに：
- 実行**予定**の記録（scheduled_at）
- CI/CD から受け取った実行**結果**の記録（実行ID・GitHub Actions Run URL 等の関連付け）
- CloudFormation Stack Event / Terraform 実行結果の保存
- **実行成功とヘルスチェック成功を分けて表示**（IaC適用成功 ≠ サービス正常）

Terraform plan の安全な取り込み経路（将来の選択肢）：CI 生成の plan を取り込む / S3 保管の plan を取り込む / 専用実行 Worker / Terraform Cloud 連携。

## 6. AWS 認証・権限（将来本番時の原則）

- AWSアクセスキーを**DBに保存しない**・ソースに**書かない**。
- ローカルは AWS プロファイルまたは Mock。AWS 上で動かす場合は **IAM Role 前提**。
- 最小権限。CloudFormation / CloudWatch / SSM の権限を分離。実行系権限と閲覧系権限を分ける。
