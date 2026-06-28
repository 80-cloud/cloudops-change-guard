package com.cloudopsguard.domain.risk;

import com.cloudopsguard.common.exception.NotFoundException;
import com.cloudopsguard.domain.policy.PolicyRule;
import com.cloudopsguard.domain.policy.PolicyRuleRepository;
import com.cloudopsguard.domain.policy.PolicyViolation;
import com.cloudopsguard.domain.policy.PolicyViolationRepository;
import com.cloudopsguard.domain.policy.dto.PolicyViolationResponse;
import com.cloudopsguard.domain.risk.dto.RiskAssessmentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * リスク判定・ポリシー違反の参照ユースケース（Phase 3・読み取り専用）。
 * 閲覧権限（所有/ロール・IDOR）は呼び出し側が {@code ChangeRequestService.getViewable} で先に担保する。
 */
@Service
public class RiskQueryService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskAssessmentFindingRepository riskFindingRepository;
    private final PolicyViolationRepository policyViolationRepository;
    private final PolicyRuleRepository policyRuleRepository;

    public RiskQueryService(RiskAssessmentRepository riskAssessmentRepository,
                            RiskAssessmentFindingRepository riskFindingRepository,
                            PolicyViolationRepository policyViolationRepository,
                            PolicyRuleRepository policyRuleRepository) {
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.riskFindingRepository = riskFindingRepository;
        this.policyViolationRepository = policyViolationRepository;
        this.policyRuleRepository = policyRuleRepository;
    }

    /** 最新のリスク判定（findings 付き）。未判定（未提出）は 404。 */
    @Transactional(readOnly = true)
    public RiskAssessmentResponse latestAssessment(Long changeRequestId) {
        RiskAssessment assessment = riskAssessmentRepository
                .findTopByChangeRequestIdOrderByAssessedAtDesc(changeRequestId)
                .orElseThrow(NotFoundException::new);
        List<RiskAssessmentFinding> findings =
                riskFindingRepository.findByRiskAssessmentId(assessment.getId());
        return RiskAssessmentResponse.from(assessment, findings);
    }

    /** ポリシー違反一覧（policy_rule_id をコードに解決）。無ければ空リスト。 */
    @Transactional(readOnly = true)
    public List<PolicyViolationResponse> policyViolations(Long changeRequestId) {
        return policyViolationRepository.findByChangeRequestId(changeRequestId).stream()
                .map(this::toResponse)
                .toList();
    }

    private PolicyViolationResponse toResponse(PolicyViolation v) {
        String code = policyRuleRepository.findById(v.getPolicyRuleId())
                .map(PolicyRule::getCode)
                .orElse(null);
        return new PolicyViolationResponse(code, v.getEffect(), v.getMessage(), v.getDetectedAt());
    }
}
