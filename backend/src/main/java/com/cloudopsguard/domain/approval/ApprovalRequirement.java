package com.cloudopsguard.domain.approval;

/**
 * 環境×リスクの承認要件（approval-flow.json の1セル）。承認・実行開始ガードが参照する。
 *
 * @param requiredApprovals       APPROVED へ進むのに必要な REVIEWER 承認数
 * @param distinctApprovers       異なる承認者であること（approvals の UNIQUE で担保される）
 * @param requireScheduledAt      承認に実施予定日時が必須か
 * @param requireRollbackProcedure 承認にロールバック手順が必須か
 * @param requirePreChecksComplete 実行開始に必須チェック完了が必要か（Phase 4 で使用）
 */
public record ApprovalRequirement(
        int requiredApprovals,
        boolean distinctApprovers,
        boolean requireScheduledAt,
        boolean requireRollbackProcedure,
        boolean requirePreChecksComplete) {
}
