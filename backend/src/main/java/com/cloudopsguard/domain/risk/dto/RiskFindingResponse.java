package com.cloudopsguard.domain.risk.dto;

import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.risk.RiskAssessmentFinding;

/**
 * リスク検知1件の応答（GET risk-assessment）。risk_findings の永続値をそのまま返す。
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
}
