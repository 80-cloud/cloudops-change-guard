# API設計（CloudOps Change Guard）

> ベースパス `/api/v1`。認可はすべてサーバー側で判定（フロント制御は補助）。レスポンス形式は統一。

## 0. 共通仕様

### 成功レスポンス
```json
{ "data": { ... }, "meta": { ... } }   // meta は一覧のページ情報等
```

### エラーレスポンス（形式統一・内部情報を漏らさない）
```json
{
  "error": {
    "code": "POLICY_BLOCKED",
    "message": "本番環境のRDS削除はポリシーにより実行できません",
    "details": [
      { "policy": "PROD_RDS_DELETE_BLOCK", "reason": "..." }
    ]
  }
}
```
- スタックトレース・SQL・内部クラス名は返さない。
- 主なHTTP：400 入力不正 / 401 未認証 / 403 認可なし / 404 不在（IDOR含む）/ 409 状態遷移・ポリシー違反 / 422 検証エラー。

### 認可マトリクス（要約）
| 操作 | REQUESTER | REVIEWER | OPERATOR | ADMIN |
|---|---|---|---|---|
| 申請作成/自分の編集 | ✅ | – | – | – |
| 全申請閲覧 | 自分のみ | ✅ | ✅ | ✅ |
| 承認/却下/差戻 | – | ✅ | – | – |
| 実行開始/前後チェック/ロールバック | – | – | ✅ | – |
| ポリシー管理/監査ログ閲覧 | – | – | – | ✅ |

---

## 1. 認証 `/api/v1/auth`
| メソッド | パス | 説明 | 権限 |
|---|---|---|---|
| POST | `/auth/login` | ログイン。access(短命)＋refresh発行 | 公開 |
| POST | `/auth/refresh` | refresh で access 再発行 | refresh保持 |
| POST | `/auth/logout` | refresh 無効化 | 認証 |
| GET | `/auth/me` | 自分の情報・ロール | 認証 |

---

## 2. 変更申請 `/api/v1/change-requests`
| メソッド | パス | 説明 | 権限 |
|---|---|---|---|
| GET | `/change-requests` | 一覧（filter: environment,status,risk,requesterId / page,size） | 認証（REQUESTERは自分のみ） |
| GET | `/change-requests/pending-approval` | 承認待ち一覧（自分が承認できるもの） | REVIEWER |
| POST | `/change-requests` | 作成（DRAFT） | REQUESTER |
| GET | `/change-requests/{id}` | 詳細（承認/チェック/監査含む・JOIN FETCH） | 認証（所有/権限検証） |
| PUT | `/change-requests/{id}` | 編集（DRAFT/RETURNEDの所有者のみ） | REQUESTER(所有者) |
| POST | `/change-requests/preview-risk` | 作成画面用リスク・ポリシーのプレビュー（非永続） | REQUESTER |

### 状態遷移エンドポイント（status を直接 PUT しない）
| メソッド | パス | 遷移 | 権限 |
|---|---|---|---|
| POST | `/change-requests/{id}/submit` | DRAFT/RETURNED→SUBMITTED | REQUESTER(所有者) |
| POST | `/change-requests/{id}/cancel` | →CANCELLED | 所有者/ADMIN |
| POST | `/change-requests/{id}/review-start` | SUBMITTED→UNDER_REVIEW | REVIEWER |
| POST | `/change-requests/{id}/approve` | →APPROVED（段数充足で確定） | REVIEWER |
| POST | `/change-requests/{id}/reject` | →REJECTED | REVIEWER |
| POST | `/change-requests/{id}/return` | →RETURNED（コメント必須） | REVIEWER |
| POST | `/change-requests/{id}/schedule` | APPROVED→SCHEDULED | OPERATOR |
| POST | `/change-requests/{id}/start` | SCHEDULED→IN_PROGRESS（前チェックガード） | OPERATOR |
| POST | `/change-requests/{id}/complete` | IN_PROGRESS→COMPLETED（IaC成功＋正常性） | OPERATOR |
| POST | `/change-requests/{id}/fail` | IN_PROGRESS→FAILED | OPERATOR |
| POST | `/change-requests/{id}/rollback` | FAILED→ROLLED_BACK | OPERATOR |

> 各遷移は許可表・ガード（[状態遷移設計.md]）を満たさなければ 409＋理由。すべて監査ログを記録。

---

## 3. リスク・ポリシー
| メソッド | パス | 説明 | 権限 |
|---|---|---|---|
| GET | `/change-requests/{id}/risk-assessment` | 最新のリスク判定結果（findings付き） | 認証（権限検証） |
| GET | `/change-requests/{id}/policy-violations` | ポリシー違反一覧 | 認証（権限検証） |
| GET | `/policies` | ポリシー一覧 | ADMIN |

---

## 4. 承認・チェック・実行
| メソッド | パス | 説明 | 権限 |
|---|---|---|---|
| GET | `/change-requests/{id}/approvals` | 承認履歴 | 認証（権限検証） |
| GET | `/change-requests/{id}/pre-checks` | 実施前チェック一覧 | 認証 |
| POST | `/change-requests/{id}/pre-checks/{checkId}/complete` | チェック完了 | OPERATOR |
| GET | `/change-requests/{id}/health-checks` | 実施後ヘルスチェック一覧 | 認証 |
| POST | `/change-requests/{id}/health-checks` | ヘルスチェック記録 | OPERATOR |
| POST | `/change-requests/{id}/execution-result` | 実行結果の記録（IaC適用結果＋外部 apply の証跡） | OPERATOR |

> `execution-result` リクエスト：`iacApplyResult`（SUCCESS / FAILED・必須）／`applyRunUrl`（外部で実行した apply の実行ログへのリンク・任意・最大2048）／`planSourceRef`（取り込んだ plan の参照・任意）。レスポンスは実行記録（実施者・適用結果・証跡・確認状況・各時刻）。Backend は apply を実行せず、外部で実行された結果を記録するだけで、記録時に監査（action_type=EXECUTION_RESULT_RECORD）を残す。

---

## 5. コメント・監査・ダッシュボード
| メソッド | パス | 説明 | 権限 |
|---|---|---|---|
| GET | `/change-requests/{id}/comments` | コメント一覧 | 認証（権限検証） |
| POST | `/change-requests/{id}/comments` | コメント追加（監査記録） | 認証 |
| GET | `/change-requests/{id}/audit-logs` | 当該申請の監査ログ | 認証（権限検証） |
| GET | `/audit-logs` | 全監査ログ（filter付き） | ADMIN |
| GET | `/dashboard/summary` | 件数/高リスク/承認待ち/実施予定/最近の監査 | 認証（ロールで表示範囲調整） |

> 監査ログに **作成系以外（PUT/DELETE）の API は設けない**（改ざん不可・受入 A-8）。
