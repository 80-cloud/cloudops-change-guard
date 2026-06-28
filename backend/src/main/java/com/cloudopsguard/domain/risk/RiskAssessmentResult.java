package com.cloudopsguard.domain.risk;

import com.cloudopsguard.domain.common.RiskLevel;

import java.util.List;

/**
 * リスク判定の集約結果（リスク判定ルール.md §1 ③）。
 * risk_level = findings の最大、isBlocked = any(isBlock)、requiresAdditionalApproval = any(...)。
 * findings が空なら LOW・非ブロック。
 */
public record RiskAssessmentResult(
        RiskLevel riskLevel,
        boolean blocked,
        boolean requiresAdditionalApproval,
        List<RiskFinding> findings) {

    /** BLOCK 該当 finding のみ（遷移拒否時の details 用）。 */
    public List<RiskFinding> blockingFindings() {
        return findings.stream().filter(RiskFinding::isBlock).toList();
    }
}
