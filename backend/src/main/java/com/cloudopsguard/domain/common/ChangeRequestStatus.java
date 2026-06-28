package com.cloudopsguard.domain.common;

/**
 * 変更申請のステータス（12種・状態遷移設計.md §1）。遷移はサーバー側の状態機械でのみ進める。
 */
public enum ChangeRequestStatus {
    DRAFT,         // 下書き（編集可）
    SUBMITTED,     // 提出済み（レビュー待ち）
    UNDER_REVIEW,  // レビュー中
    APPROVED,      // 承認済み
    REJECTED,      // 却下（終端）
    RETURNED,      // 差し戻し（編集可）
    SCHEDULED,     // 実施予定確定
    IN_PROGRESS,   // 実施中
    COMPLETED,     // 完了（IaC成功＋正常性確認済み・終端）
    FAILED,        // 実施失敗
    ROLLED_BACK,   // ロールバック済み（終端）
    CANCELLED;     // 取消（終端）

    /** 編集可能な状態（DRAFT / RETURNED のみ・状態遷移設計.md §4）。 */
    public boolean isEditable() {
        return this == DRAFT || this == RETURNED;
    }

    /** 終端状態（これ以上遷移しない）。 */
    public boolean isTerminal() {
        return this == REJECTED || this == COMPLETED || this == ROLLED_BACK || this == CANCELLED;
    }
}
