package com.cloudopsguard.domain.common;

/**
 * 監査ログの操作種別（audit_logs.action_type）。ER図.md §2-12 の集合に、状態遷移設計.md §4
 * 「すべての遷移で監査ログを記録」を満たすため CANCEL / REVIEW_START / SCHEDULE を加えた上位集合。
 * 列は VARCHAR(30) で自由なため追加に migration は不要。追記専用・編集権限は誰にも与えない。
 */
public enum AuditAction {
    CREATE,
    EDIT,
    SUBMIT,
    REVIEW_START,
    APPROVE,
    REJECT,
    RETURN,
    SCHEDULE,
    CANCEL,
    EXECUTION_START,
    EXECUTION_COMPLETE,
    EXECUTION_FAIL,
    ROLLBACK,
    POLICY_VIOLATION,
    COMMENT
}
