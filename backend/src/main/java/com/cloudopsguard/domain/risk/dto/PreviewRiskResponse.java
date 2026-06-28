package com.cloudopsguard.domain.risk.dto;

import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.policy.PolicyEffect;
import com.cloudopsguard.domain.policy.PolicyOutcome;
import com.cloudopsguard.domain.risk.AssessmentOutcome;
import com.cloudopsguard.domain.risk.BlockReason;
import com.cloudopsguard.domain.risk.RiskAssessmentResult;
import com.cloudopsguard.domain.risk.RiskFinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 作成画面用リスク・ポリシーのプレビュー応答（POST /change-requests/preview-risk・非永続）。
 * 集約結果＋検知明細＋ブロック理由を返す（GET risk-assessment と整合する形）。
 */
public record PreviewRiskResponse(
        RiskLevel riskLevel,
        boolean blocked,
        boolean requiresAdditionalApproval,
        List<RiskFindingResponse> findings,
        List<BlockReason> blockReasons) {

    public static PreviewRiskResponse from(AssessmentOutcome outcome) {
        RiskAssessmentResult risk = outcome.risk();
        boolean additional = risk.requiresAdditionalApproval()
                || outcome.policy().requiresAdditionalApproval()
                || outcome.policy().requiresDualApproval();

        List<RiskFindingResponse> findings = risk.findings().stream()
                .map(RiskFindingResponse::from)
                .toList();

        List<BlockReason> blockReasons = new ArrayList<>();
        for (RiskFinding f : risk.blockingFindings()) {
            blockReasons.add(new BlockReason("RISK", f.ruleCode(), f.whyDangerous(), f.recommendedAction()));
        }
        for (PolicyOutcome o : outcome.policy().outcomes()) {
            if (o.effect() == PolicyEffect.BLOCK) {
                blockReasons.add(new BlockReason("POLICY", o.policyCode(), o.message(), null));
            }
        }
        return new PreviewRiskResponse(risk.riskLevel(), outcome.blocked(), additional, findings, blockReasons);
    }
}
