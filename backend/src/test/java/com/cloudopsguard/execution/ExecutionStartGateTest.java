package com.cloudopsguard.execution;

import com.cloudopsguard.common.exception.IllegalStateTransitionException;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.common.TransitionAction;
import com.cloudopsguard.domain.execution.ExecutionService;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Inc 3：START ゲート（A-7）の結合テスト。 */
class ExecutionStartGateTest extends AbstractIntegrationTest {

    @Autowired ChangeRequestService changeRequestService;
    @Autowired ExecutionService executionService;

    private ChangeRequest scheduledCr(Environment env, RiskLevel risk) {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.SCHEDULED);
        cr.setTargetEnvironment(env);
        cr.setRiskLevel(risk);
        cr = changeRequestRepository.save(cr);
        executionService.instantiatePreChecks(cr);
        return cr;
    }

    @Test
    void 本番CRは必須preCheck未完了だとSTARTが409でSCHEDULEDのまま() {
        ChangeRequest cr = scheduledCr(Environment.PRODUCTION, RiskLevel.HIGH);
        User operator = createUser("op", Role.OPERATOR);

        assertThatThrownBy(() ->
                changeRequestService.transition(principal(operator), cr.getId(), TransitionAction.START, null))
                .isInstanceOf(IllegalStateTransitionException.class);

        assertThat(changeRequestRepository.findById(cr.getId()).orElseThrow().getStatus())
                .isEqualTo(ChangeRequestStatus.SCHEDULED);
        assertThat(executionRepository.count()).isZero();
    }

    @Test
    void 本番CRは必須preCheck全完了でIN_PROGRESSになりexecutionが記録される() {
        ChangeRequest cr = scheduledCr(Environment.PRODUCTION, RiskLevel.HIGH);
        User operator = createUser("op", Role.OPERATOR);
        executionService.listPreChecks(cr.getId()).forEach(pc ->
                executionService.completePreCheck(principal(operator), cr.getId(), pc.id()));

        ChangeRequest started = changeRequestService.transition(
                principal(operator), cr.getId(), TransitionAction.START, null);

        assertThat(started.getStatus()).isEqualTo(ChangeRequestStatus.IN_PROGRESS);
        assertThat(executionRepository.count()).isEqualTo(1);
    }

    @Test
    void 開発CRはpreCheck未完了でもSTARTできる() {
        ChangeRequest cr = scheduledCr(Environment.DEVELOPMENT, RiskLevel.LOW);
        User operator = createUser("op", Role.OPERATOR);

        ChangeRequest started = changeRequestService.transition(
                principal(operator), cr.getId(), TransitionAction.START, null);

        assertThat(started.getStatus()).isEqualTo(ChangeRequestStatus.IN_PROGRESS);
    }
}
