# ER図・テーブル設計（CloudOps Change Guard）

> Phase 1 設計。物理名は snake_case。すべて Flyway で管理し、後方互換を保って変更する。

## 1. エンティティ関係図（概念）

```
users (1) ──< change_requests (申請者 requester_id)
                  │
                  ├──< risk_assessments (1:1 最新) ──< risk_findings
                  ├──< policy_violations >── policy_rules
                  ├──< approvals          >── users (reviewer_id)
                  ├──< pre_execution_checks
                  ├──< post_execution_health_checks
                  ├──< executions         >── users (operator_id)
                  ├──< comments           >── users (author_id)
                  └──< audit_logs         >── users (actor_id)

users (1) ──< refresh_tokens （ログインセッション・認証）
```

凡例：`──<` は 1対多。`>──` は多対1。

---

## 2. テーブル定義

### 2-1. users（利用者）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| username | VARCHAR(50) | UNIQUE NOT NULL | ログインID |
| email | VARCHAR(255) | UNIQUE NOT NULL | |
| password_hash | VARCHAR(255) | NOT NULL | bcrypt |
| role | VARCHAR(20) | NOT NULL | REQUESTER / REVIEWER / OPERATOR / ADMIN |
| display_name | VARCHAR(100) | NOT NULL | |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

> **Role の扱い**：MVP は単一ロール/ユーザーを `users.role`（enum: REQUESTER/REVIEWER/OPERATOR/ADMIN）で表現する。Role を独立エンティティにしたい場合は `roles(id, code, name)` ＋ `user_roles(user_id, role_id)` 中間表で M:N 拡張できる（将来・今回は対象外）。本書では enum 方式を採用。

### 2-1b. refresh_tokens（リフレッシュトークン）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| user_id | BIGINT | FK→users NOT NULL | 所有ユーザー |
| token_hash | VARCHAR(64) | UNIQUE NOT NULL | 生トークンは保存せず SHA-256 hex(64) を保持 |
| expires_at | TIMESTAMPTZ | NOT NULL | 失効期限 |
| revoked_at | TIMESTAMPTZ | NULL | 失効時刻。NULL=有効 |
| created_at | TIMESTAMPTZ | NOT NULL | |

インデックス：`(user_id)`。

> 短命 access トークン（JWT）＋ refresh の DB ローテーションで認証を保つ。refresh のたびに使い捨て発行し直し、失効済みトークンの再提示を検知したら当該ユーザーの全トークンを失効させる（盗用対策）。

### 2-2. change_requests（変更申請・中核）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| title | VARCHAR(200) | NOT NULL | タイトル |
| target_environment | VARCHAR(20) | NOT NULL | development / staging / production |
| iac_type | VARCHAR(20) | NOT NULL | CLOUDFORMATION / TERRAFORM |
| target_aws_service | VARCHAR(50) | NOT NULL | 例: RDS, S3, EC2, IAM, ALB |
| target_resource_name | VARCHAR(200) | NOT NULL | 対象リソース名 |
| change_reason | TEXT | NOT NULL | 変更理由 |
| change_summary | TEXT | NOT NULL | 変更概要 |
| diff_text | TEXT | NOT NULL | 変更差分テキスト |
| scheduled_at | TIMESTAMPTZ | NULL | 実施予定日時 |
| rollback_procedure | TEXT | NULL | ロールバック手順 |
| status | VARCHAR(20) | NOT NULL | [状態遷移設計.md] 参照。既定 DRAFT |
| risk_level | VARCHAR(10) | NULL | LOW/MEDIUM/HIGH/CRITICAL（最新判定のキャッシュ） |
| requester_id | BIGINT | FK→users NOT NULL | 申請者 |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

インデックス：`(status)`, `(target_environment)`, `(risk_level)`, `(requester_id)`, `(scheduled_at)`。

> 承認者・実施者は単一カラムにせず、approvals / executions で多重に保持する（CRITICAL 二重承認・再実行に対応）。

### 2-3. risk_assessments（リスク判定の実行単位）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| change_request_id | BIGINT | FK NOT NULL | |
| risk_level | VARCHAR(10) | NOT NULL | findings の最大値 |
| is_blocked | BOOLEAN | NOT NULL | BLOCK 該当ありか |
| requires_additional_approval | BOOLEAN | NOT NULL | 追加承認要否 |
| assessed_at | TIMESTAMPTZ | NOT NULL | |

### 2-4. risk_findings（個別の検知結果）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| risk_assessment_id | BIGINT | FK NOT NULL | |
| rule_code | VARCHAR(50) | NOT NULL | 検知ルールコード |
| rule_name | VARCHAR(100) | NOT NULL | 検知ルール名 |
| risk_level | VARCHAR(10) | NOT NULL | このルールのレベル |
| why_dangerous | TEXT | NOT NULL | なぜ危険か |
| expected_impact | TEXT | NOT NULL | 想定される影響 |
| recommended_action | TEXT | NOT NULL | 推奨対応 |
| is_block | BOOLEAN | NOT NULL | ブロック対象か |
| requires_additional_approval | BOOLEAN | NOT NULL | 追加承認が必要か |

### 2-5. policy_rules（ポリシー定義）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| code | VARCHAR(50) | UNIQUE NOT NULL | 例: PROD_RDS_DELETE_BLOCK |
| name | VARCHAR(150) | NOT NULL | |
| description | TEXT | NOT NULL | なぜ実行できないか/必要かの説明文 |
| environment_scope | VARCHAR(20) | NOT NULL | development/staging/production/ALL |
| effect | VARCHAR(40) | NOT NULL | BLOCK / REQUIRE_DUAL_APPROVAL / REQUIRE_ADDITIONAL_APPROVAL / REQUIRE_REASON / REQUIRE_MAINTENANCE_WINDOW |
| enabled | BOOLEAN | NOT NULL | |
| created_at | TIMESTAMPTZ | NOT NULL | |

> 条件はコード側のポリシー評価器（[ポリシー一覧.md]）で判定。MVP は初期データ投入＋一覧表示。

### 2-6. policy_violations（ポリシー違反の記録）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| change_request_id | BIGINT | FK NOT NULL | |
| policy_rule_id | BIGINT | FK NOT NULL | |
| effect | VARCHAR(40) | NOT NULL | 適用された効果 |
| message | TEXT | NOT NULL | 画面/APIに返す理由文 |
| detected_at | TIMESTAMPTZ | NOT NULL | |

### 2-7. approvals（承認・却下・差し戻し）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| change_request_id | BIGINT | FK NOT NULL | |
| reviewer_id | BIGINT | FK→users NOT NULL | レビュー者 |
| decision | VARCHAR(20) | NOT NULL | APPROVED / REJECTED / RETURNED |
| comment | TEXT | NULL | |
| decided_at | TIMESTAMPTZ | NOT NULL | |

制約：`UNIQUE(change_request_id, reviewer_id)` で同一レビュー者の二重承認を防ぐ。サーバー側で `reviewer_id ≠ requester_id`（自己承認禁止）を検証。

### 2-8. pre_execution_checks（実施前チェック）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| change_request_id | BIGINT | FK NOT NULL | |
| check_type | VARCHAR(50) | NOT NULL | BACKUP / ROLLBACK / MONITORING / IMPACT / STAKEHOLDER / WINDOW / APPROVAL |
| is_required | BOOLEAN | NOT NULL | 本番では必須項目 true |
| is_completed | BOOLEAN | NOT NULL | |
| completed_by | BIGINT | FK→users NULL | |
| completed_at | TIMESTAMPTZ | NULL | |

### 2-9. post_execution_health_checks（実施後ヘルスチェック）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| change_request_id | BIGINT | FK NOT NULL | |
| check_item | VARCHAR(60) | NOT NULL | IAC_APPLY / ALB_TARGET_HEALTH / EC2_SSM / HTTP_HEALTH / CW_ALARM / APP_REACHABILITY / DB_CONNECTION / NOTE |
| result | VARCHAR(12) | NOT NULL | HEALTHY / WARNING / UNHEALTHY / NOT_CHECKED（正常/警告/異常/未確認） |
| note | TEXT | NULL | |
| recorded_by | BIGINT | FK→users NOT NULL | |
| recorded_at | TIMESTAMPTZ | NOT NULL | |

### 2-10. executions（実行記録）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| change_request_id | BIGINT | FK NOT NULL | |
| operator_id | BIGINT | FK→users NOT NULL | 実施者 |
| iac_apply_result | VARCHAR(10) | NULL | SUCCESS / FAILED |
| service_health_confirmed | BOOLEAN | NOT NULL DEFAULT false | サービス正常性確認済みか（IaC成功とは別概念） |
| started_at | TIMESTAMPTZ | NOT NULL | |
| finished_at | TIMESTAMPTZ | NULL | |
| rollback_performed | BOOLEAN | NOT NULL DEFAULT false | |
| rollback_note | TEXT | NULL | |
| apply_run_url | TEXT | NULL | 外部で実行した apply の実行ログ等へのリンク（A-2b） |
| plan_source_ref | TEXT | NULL | 取り込んだ plan の参照（A-2b） |

> 「IaC適用成功（iac_apply_result=SUCCESS）」と「サービス正常性確認済み（service_health_confirmed=true）」を別カラムで保持し、COMPLETED への遷移条件で区別する。

### 2-11. comments（コメント）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| change_request_id | BIGINT | FK NOT NULL | |
| author_id | BIGINT | FK→users NOT NULL | |
| body | TEXT | NOT NULL | |
| created_at | TIMESTAMPTZ | NOT NULL | |

### 2-12. audit_logs（監査ログ・追記専用）
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| change_request_id | BIGINT | FK NULL | 対象変更申請（ポリシー定義操作等は NULL 可） |
| actor_id | BIGINT | FK→users NOT NULL | 操作者 |
| action_type | VARCHAR(30) | NOT NULL | 下記の種別 |
| before_status | VARCHAR(20) | NULL | 変更前状態 |
| after_status | VARCHAR(20) | NULL | 変更後状態 |
| comment | TEXT | NULL | |
| summary | TEXT | NULL | 変更内容の要約 |
| created_at | TIMESTAMPTZ | NOT NULL | |

`action_type`：CREATE / EDIT / SUBMIT / REVIEW_START / APPROVE / REJECT / RETURN / SCHEDULE / CANCEL / EXECUTION_START / EXECUTION_COMPLETE / EXECUTION_FAIL / ROLLBACK / EXECUTION_RESULT_RECORD / POLICY_VIOLATION / COMMENT。

> **改ざん防止**：監査ログは Service 層で INSERT のみ許可。UPDATE / DELETE を行うリポジトリメソッドを設けない。アプリのロールに監査ログ編集権限を一切与えない（IPアドレスは今回保持しない）。

---

## 3. 設計上の判断メモ

- **承認者/実施者を申請テーブルの単一カラムにしない**：CRITICAL の二重承認・再実行を素直に表現するため別テーブル化。
- **risk_level は change_requests にキャッシュ**しつつ、正本は risk_assessments/risk_findings に持つ（一覧の絞り込み性能 P-2/P-3 と整合性の両立）。
- **ソフトデリート**：取消は CANCELLED ステータス。物理削除しない（監査の連続性）。
