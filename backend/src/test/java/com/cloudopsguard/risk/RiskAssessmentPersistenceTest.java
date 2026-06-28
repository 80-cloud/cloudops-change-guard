package com.cloudopsguard.risk;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.policy.PolicyEffect;
import com.cloudopsguard.domain.policy.PolicyRule;
import com.cloudopsguard.domain.policy.PolicyViolation;
import com.cloudopsguard.domain.risk.AssessmentOutcome;
import com.cloudopsguard.domain.risk.RiskAssessment;
import com.cloudopsguard.domain.risk.RiskAssessmentFinding;
import com.cloudopsguard.domain.risk.RiskAssessmentService;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * リスク判定＋ポリシー評価の永続化（Increment 3）の結合テスト。Testcontainers の実 PostgreSQL を使う。
 * risk_assessments / risk_findings / policy_violations への書き込みと、change_requests.risk_level 更新を検証。
 */
class RiskAssessmentPersistenceTest extends AbstractIntegrationTest {

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @BeforeEach
    void seedPolicies() {
        // 評価器は JSON 駆動だが、policy_violations の FK 先として DB の policy_rules が要る。
        policyRuleRepository.deleteAll();
        savePolicy("PROD_RDS_DELETE_BLOCK", "production", "BLOCK");
        savePolicy("CRITICAL_DUAL_APPROVAL", "ALL", "REQUIRE_DUAL_APPROVAL");
    }

    private void savePolicy(String code, String scope, String effect) {
        PolicyRule rule = new PolicyRule();
        rule.setCode(code);
        rule.setName(code);
        rule.setDescription(code);
        rule.setEnvironmentScope(scope);
        rule.setEffect(effect);
        rule.setEnabled(true);
        policyRuleRepository.save(rule);
    }

    private ChangeRequest changeRequestWith(User requester, Environment env, String diff) {
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);
        cr.setTargetEnvironment(env);
        cr.setDiffText(diff);
        return changeRequestRepository.save(cr);
    }

    @Test
    void 本番RDS削除はCRITICALで永続化されブロック_違反も記録() {
        User requester = createUser("req1", Role.REQUESTER);
        ChangeRequest cr = changeRequestWith(requester, Environment.PRODUCTION,
                "# aws_db_instance.main will be destroyed");

        AssessmentOutcome outcome = riskAssessmentService.assess(cr);

        assertThat(outcome.blocked()).isTrue();
        assertThat(outcome.risk().riskLevel()).isEqualTo(RiskLevel.CRITICAL);

        List<RiskAssessment> assessments = riskAssessmentRepository.findAll();
        assertThat(assessments).hasSize(1);
        assertThat(assessments.get(0).getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(assessments.get(0).isBlocked()).isTrue();

        assertThat(riskAssessmentFindingRepository.findAll())
                .extracting(RiskAssessmentFinding::getRuleCode)
                .contains("RDS_DELETE_OR_REPLACE");

        assertThat(policyViolationRepository.findAll())
                .extracting(PolicyViolation::getEffect)
                .contains(PolicyEffect.BLOCK);

        ChangeRequest reloaded = changeRequestRepository.findById(cr.getId()).orElseThrow();
        assertThat(reloaded.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void 開発の無害変更は検知なしでブロックされず違反も無い() {
        User requester = createUser("req1", Role.REQUESTER);
        ChangeRequest cr = changeRequestWith(requester, Environment.DEVELOPMENT,
                "- instance_class = db.t3.medium\n+ instance_class = db.t3.large");

        AssessmentOutcome outcome = riskAssessmentService.assess(cr);

        assertThat(outcome.blocked()).isFalse();
        assertThat(outcome.risk().findings()).isEmpty();
        assertThat(outcome.risk().riskLevel()).isEqualTo(RiskLevel.LOW);

        assertThat(riskAssessmentFindingRepository.findAll()).isEmpty();
        assertThat(policyViolationRepository.findAll()).isEmpty();

        ChangeRequest reloaded = changeRequestRepository.findById(cr.getId()).orElseThrow();
        assertThat(reloaded.getRiskLevel()).isEqualTo(RiskLevel.LOW);
    }
}
