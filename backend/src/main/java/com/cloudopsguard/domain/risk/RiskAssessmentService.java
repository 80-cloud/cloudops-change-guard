package com.cloudopsguard.domain.risk;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestRepository;
import com.cloudopsguard.domain.changerequest.dto.CreateChangeRequest;
import com.cloudopsguard.domain.policy.PolicyEvaluationResult;
import com.cloudopsguard.domain.policy.PolicyEngine;
import com.cloudopsguard.domain.policy.PolicyRuleRepository;
import com.cloudopsguard.domain.policy.PolicyViolation;
import com.cloudopsguard.domain.policy.PolicyViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * リスク判定＋ポリシー評価を実行し、結果を永続化するユースケース（Phase 3）。
 *
 * <p>{@link RiskEngine} と {@link PolicyEngine}（純ロジック）を呼び、risk_assessments / risk_findings /
 * policy_violations へ書き、change_requests.risk_level（最新判定キャッシュ）を更新する。
 * 遷移そのものは行わない——SUBMIT ガードへの配線は呼び出し側（ChangeRequestService）で行う。
 */
@Service
public class RiskAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentService.class);

    private final RiskEngine riskEngine;
    private final PolicyEngine policyEngine;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskAssessmentFindingRepository riskFindingRepository;
    private final PolicyViolationRepository policyViolationRepository;
    private final PolicyRuleRepository policyRuleRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final IaCChangeProvider iaCChangeProvider;

    public RiskAssessmentService(RiskEngine riskEngine, PolicyEngine policyEngine,
                                 RiskAssessmentRepository riskAssessmentRepository,
                                 RiskAssessmentFindingRepository riskFindingRepository,
                                 PolicyViolationRepository policyViolationRepository,
                                 PolicyRuleRepository policyRuleRepository,
                                 ChangeRequestRepository changeRequestRepository,
                                 IaCChangeProvider iaCChangeProvider) {
        this.riskEngine = riskEngine;
        this.policyEngine = policyEngine;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.riskFindingRepository = riskFindingRepository;
        this.policyViolationRepository = policyViolationRepository;
        this.policyRuleRepository = policyRuleRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.iaCChangeProvider = iaCChangeProvider;
    }

    /** 判定して永続化し、総合結果を返す。呼び出し側のトランザクションに参加する。 */
    @Transactional
    public AssessmentOutcome assess(ChangeRequest cr) {
        RiskAssessmentResult risk = riskEngine.assess(cr);
        PolicyEvaluationResult policy = policyEngine.evaluate(cr, risk);
        boolean blocked = risk.blocked() || policy.blocked();

        RiskAssessment assessment = new RiskAssessment();
        assessment.setChangeRequestId(cr.getId());
        assessment.setRiskLevel(risk.riskLevel());
        assessment.setBlocked(blocked);
        assessment.setRequiresAdditionalApproval(
                risk.requiresAdditionalApproval()
                        || policy.requiresAdditionalApproval()
                        || policy.requiresDualApproval());
        RiskAssessment savedAssessment = riskAssessmentRepository.save(assessment);

        for (RiskFinding f : risk.findings()) {
            RiskAssessmentFinding row = new RiskAssessmentFinding();
            row.setRiskAssessmentId(savedAssessment.getId());
            row.setRuleCode(f.ruleCode());
            row.setRuleName(f.ruleName());
            row.setRiskLevel(f.riskLevel());
            row.setWhyDangerous(f.whyDangerous());
            row.setExpectedImpact(f.expectedImpact());
            row.setRecommendedAction(f.recommendedAction());
            row.setBlock(f.isBlock());
            row.setRequiresAdditionalApproval(f.requiresAdditionalApproval());
            riskFindingRepository.save(row);
        }

        // policy_violations は policy_rule への FK が要る。未 seed のコードは行を残せないため記録のみ。
        policy.outcomes().forEach(outcome ->
                policyRuleRepository.findByCode(outcome.policyCode()).ifPresentOrElse(rule -> {
                    PolicyViolation violation = new PolicyViolation();
                    violation.setChangeRequestId(cr.getId());
                    violation.setPolicyRuleId(rule.getId());
                    violation.setEffect(outcome.effect());
                    violation.setMessage(outcome.message());
                    policyViolationRepository.save(violation);
                }, () -> log.warn("policy_rules 未登録のため違反を永続化できません: {}", outcome.policyCode())));

        cr.setRiskLevel(risk.riskLevel());
        changeRequestRepository.save(cr);

        return new AssessmentOutcome(risk, policy, blocked);
    }

    /** 非永続プレビュー（作成画面用・API設計.md §2）。判定のみで永続化しない。 */
    public AssessmentOutcome preview(CreateChangeRequest req) {
        ChangeRequest cr = new ChangeRequest();
        cr.setTargetEnvironment(req.targetEnvironment());
        cr.setIacType(req.iacType());
        cr.setTargetAwsService(req.targetAwsService());
        cr.setTargetResourceName(req.targetResourceName());
        cr.setDiffText(iaCChangeProvider.resolveDiffText(req.planSourceRef(), req.diffText()));
        RiskAssessmentResult risk = riskEngine.assess(cr);
        PolicyEvaluationResult policy = policyEngine.evaluate(cr, risk);
        return new AssessmentOutcome(risk, policy, risk.blocked() || policy.blocked());
    }

    /** 最新判定が追加承認（リスク/ポリシー由来）を要求するか（定足数 +1 判定に使う）。未判定は false。 */
    @Transactional(readOnly = true)
    public boolean requiresAdditionalApproval(Long changeRequestId) {
        return riskAssessmentRepository.findTopByChangeRequestIdOrderByAssessedAtDesc(changeRequestId)
                .map(RiskAssessment::isRequiresAdditionalApproval)
                .orElse(false);
    }
}
