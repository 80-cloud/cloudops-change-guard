package com.cloudopsguard.domain.common;

/**
 * 利用者ロール（RBAC の主体・権限マトリクス.md §1）。MVP は 1ユーザー1ロール。
 * Spring Security の権限へは {@code ROLE_<name>} で写す（例 ROLE_REVIEWER）。
 */
public enum Role {
    REQUESTER,   // 変更申請の作成・自分の申請の編集/提出
    REVIEWER,    // レビュー着手・承認・却下・差し戻し（自己承認は不可）
    OPERATOR,    // 承認済み変更の実施・前後チェック・ロールバック
    ADMIN        // 全申請閲覧・ポリシー管理・監査ログ閲覧
}
