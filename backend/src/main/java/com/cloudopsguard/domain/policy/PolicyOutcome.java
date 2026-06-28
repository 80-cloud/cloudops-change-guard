package com.cloudopsguard.domain.policy;

/**
 * ポリシー評価で適用された1件（policy_violations へ写す元データ）。
 *
 * @param policyCode policy-rules.json の code（DB の policy_rules.code と対応）
 * @param effect     適用された効果
 * @param message    利用者向けメッセージ
 */
public record PolicyOutcome(String policyCode, PolicyEffect effect, String message) {
}
