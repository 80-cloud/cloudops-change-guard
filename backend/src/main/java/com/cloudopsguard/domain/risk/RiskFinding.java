package com.cloudopsguard.domain.risk;

import com.cloudopsguard.domain.common.RiskLevel;

/**
 * リスク検知の1件（リスク判定ルール.md §4：赤黄緑で終わらせず根拠ある説明文を返す）。
 * risk_findings テーブルへ写す元データ（永続化は Phase 3 後半）。
 */
public record RiskFinding(
        String ruleCode,
        String ruleName,
        RiskLevel riskLevel,
        String whyDangerous,
        String plainMeaning,
        String expectedImpact,
        String recommendedAction,
        boolean isBlock,
        boolean requiresAdditionalApproval) {
}
