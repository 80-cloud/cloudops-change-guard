package com.cloudopsguard.domain.policy;

/**
 * ポリシーの効果（ポリシー一覧.md・policy_rules.effect）。
 * BLOCK は遷移を止め、それ以外は承認段数・理由・メンテ枠などの追加要件として扱う。
 */
public enum PolicyEffect {
    BLOCK,
    REQUIRE_DUAL_APPROVAL,
    REQUIRE_ADDITIONAL_APPROVAL,
    REQUIRE_REASON,
    REQUIRE_MAINTENANCE_WINDOW
}
