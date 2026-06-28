package com.cloudopsguard.domain.risk.dto;

import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.risk.RiskAssessmentFinding;
import com.cloudopsguard.domain.risk.RiskFinding;

/**
 * リスク検知1件の応答（GET risk-assessment / preview-risk）。
 * 永続値（{@link RiskAssessmentFinding}）と判定結果（{@link RiskFinding}）の双方から組み立てられる。
 */
public record RiskFindingResponse(
        String ruleCode,
        String ruleName,
        RiskLevel riskLevel,
        String whyDangerous,
        String expectedImpact,
        String recommendedAction,
        boolean isBlock,
        boolean requiresAdditionalApproval) {

    public static RiskFindingResponse from(RiskAssessmentFinding f) {
        return new RiskFindingResponse(
                f.getRuleCode(), f.getRuleName(), f.getRiskLevel(),
                f.getWhyDangerous(), f.getExpectedImpact(), f.getRecommendedAction(),
                f.isBlock(), f.isRequiresAdditionalApproval());
    }

    public static RiskFindingResponse from(RiskFinding f) {
        return new RiskFindingResponse(
                f.ruleCode(), f.ruleName(), f.riskLevel(),
                f.whyDangerous(), f.expectedImpact(), f.recommendedAction(),
                f.isBlock(), f.requiresAdditionalApproval());
    }
}
