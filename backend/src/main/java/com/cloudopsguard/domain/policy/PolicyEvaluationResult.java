package com.cloudopsguard.domain.policy;

import java.util.List;

/**
 * ポリシー評価の集約結果。BLOCK の有無と、承認段数・理由・メンテ枠などの追加要件を導出する。
 */
public record PolicyEvaluationResult(List<PolicyOutcome> outcomes) {

    private boolean hasEffect(PolicyEffect effect) {
        return outcomes.stream().anyMatch(o -> o.effect() == effect);
    }

    /** BLOCK 効果が1件でもあるか（遷移を止める）。 */
    public boolean blocked() {
        return hasEffect(PolicyEffect.BLOCK);
    }

    public boolean requiresDualApproval() {
        return hasEffect(PolicyEffect.REQUIRE_DUAL_APPROVAL);
    }

    public boolean requiresAdditionalApproval() {
        return hasEffect(PolicyEffect.REQUIRE_ADDITIONAL_APPROVAL);
    }

    public boolean requiresReason() {
        return hasEffect(PolicyEffect.REQUIRE_REASON);
    }

    public boolean requiresMaintenanceWindow() {
        return hasEffect(PolicyEffect.REQUIRE_MAINTENANCE_WINDOW);
    }
}
