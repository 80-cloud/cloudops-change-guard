package com.cloudopsguard.domain.risk.dto;

import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.risk.RiskAssessment;
import com.cloudopsguard.domain.risk.RiskAssessmentFinding;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 最新リスク判定の応答（GET /change-requests/{id}/risk-assessment）。集約結果＋明細の説明文を返す。
 */
public record RiskAssessmentResponse(
        RiskLevel riskLevel,
        boolean blocked,
        boolean requiresAdditionalApproval,
        OffsetDateTime assessedAt,
        List<RiskFindingResponse> findings) {

    public static RiskAssessmentResponse from(RiskAssessment ra, List<RiskAssessmentFinding> findings) {
        return new RiskAssessmentResponse(
                ra.getRiskLevel(), ra.isBlocked(), ra.isRequiresAdditionalApproval(), ra.getAssessedAt(),
                findings.stream().map(RiskFindingResponse::from).toList());
    }
}
