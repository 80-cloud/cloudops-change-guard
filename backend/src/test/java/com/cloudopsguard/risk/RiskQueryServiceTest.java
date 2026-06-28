package com.cloudopsguard.risk;

import com.cloudopsguard.common.exception.NotFoundException;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.policy.PolicyRule;
import com.cloudopsguard.domain.policy.dto.PolicyViolationResponse;
import com.cloudopsguard.domain.risk.RiskAssessmentService;
import com.cloudopsguard.domain.risk.RiskQueryService;
import com.cloudopsguard.domain.risk.dto.RiskAssessmentResponse;
import com.cloudopsguard.domain.risk.dto.RiskFindingResponse;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * リスク参照 API のクエリ層（Increment 6）。最新判定・findings・ポリシー違反の取得を検証する。
 * 閲覧権限（getViewable）は Controller の責務で、ChangeRequestAuthorizationTest がカバー済み。
 */
class RiskQueryServiceTest extends AbstractIntegrationTest {

    @Autowired
    private RiskQueryService riskQueryService;
    @Autowired
    private RiskAssessmentService riskAssessmentService;

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

    private ChangeRequest prodRdsDelete(User requester) {
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);
        cr.setTargetEnvironment(Environment.PRODUCTION);
        cr.setDiffText("# aws_db_instance.main will be destroyed");
        return changeRequestRepository.save(cr);
    }

    @Test
    void 最新判定は説明文付きのfindingsを返す() {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = prodRdsDelete(requester);
        riskAssessmentService.assess(cr);

        RiskAssessmentResponse resp = riskQueryService.latestAssessment(cr.getId());

        assertThat(resp.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(resp.blocked()).isTrue();
        assertThat(resp.findings()).extracting(RiskFindingResponse::ruleCode)
                .contains("RDS_DELETE_OR_REPLACE");
        // 赤黄緑で終わらせず、根拠ある説明文を返す（リスク判定ルール.md §4）。
        assertThat(resp.findings().get(0).whyDangerous()).isNotBlank();
    }

    @Test
    void ポリシー違反はコードに解決して返す() {
        savePolicy("PROD_RDS_DELETE_BLOCK", "production", "BLOCK");
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = prodRdsDelete(requester);
        riskAssessmentService.assess(cr);

        List<PolicyViolationResponse> violations = riskQueryService.policyViolations(cr.getId());

        assertThat(violations).extracting(PolicyViolationResponse::code)
                .contains("PROD_RDS_DELETE_BLOCK");
    }

    @Test
    void 未判定の申請はリスク判定が404になる() {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);   // assess していない

        assertThatThrownBy(() -> riskQueryService.latestAssessment(cr.getId()))
                .isInstanceOf(NotFoundException.class);
    }
}
