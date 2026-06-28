package com.cloudopsguard.domain.risk;

import com.cloudopsguard.domain.policy.PolicyEvaluationResult;

/**
 * リスク判定＋ポリシー評価の総合結果（SUBMIT ガード等の呼び出し側が使う）。
 * blocked はリスク側 isBlock とポリシー側 BLOCK の論理和（fail-closed）。
 */
public record AssessmentOutcome(
        RiskAssessmentResult risk,
        PolicyEvaluationResult policy,
        boolean blocked) {
}
