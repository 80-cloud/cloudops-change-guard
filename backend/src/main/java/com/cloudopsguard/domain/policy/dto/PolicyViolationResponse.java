package com.cloudopsguard.domain.policy.dto;

import com.cloudopsguard.domain.policy.PolicyEffect;

import java.time.OffsetDateTime;

/**
 * ポリシー違反1件の応答（GET /change-requests/{id}/policy-violations）。
 * policy_rule_id はコード（policy_rules.code）に解決して返す（API設計.md §0 の details 形式に合わせる）。
 */
public record PolicyViolationResponse(
        String code,
        PolicyEffect effect,
        String message,
        OffsetDateTime detectedAt) {
}
