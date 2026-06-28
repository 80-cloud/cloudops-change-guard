package com.cloudopsguard.execution;

import com.cloudopsguard.common.exception.IllegalStateTransitionException;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.changerequest.dto.TransitionRequest;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.common.TransitionAction;
import com.cloudopsguard.domain.execution.Execution;
import com.cloudopsguard.domain.execution.ExecutionService;
import com.cloudopsguard.domain.execution.HealthCheckItem;
import com.cloudopsguard.domain.execution.HealthResult;
import com.cloudopsguard.domain.execution.IacApplyResult;
import com.cloudopsguard.domain.execution.dto.CreateHealthCheck;
import com.cloudopsguard.domain.execution.dto.RecordExecutionResult;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.security.AppUserPrincipal;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Inc 4：COMPLETE（A-10）・FAIL・ROLLBACK の結合テスト。 */
class ExecutionCompleteTest extends AbstractIntegrationTest {

    @Autowired ChangeRequestService changeRequestService;
    @Autowired ExecutionService executionService;

    private User operator;

    private ChangeRequest inProgressCr() {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.IN_PROGRESS);
        operator = createUser("op", Role.OPERATOR);
        executionService.startExecution(cr, principal(operator));   // execution を1行用意
        return cr;
    }

    private void recordHealth(ChangeRequest cr, HealthCheckItem item, HealthResult result) {
        executionService.recordHealthCheck(principal(operator), cr.getId(),
                new CreateHealthCheck(item, result, null));
    }

    @Test
    void iac成功かつ必須ヘルス全HEALTHYでCOMPLETEできる() {
        ChangeRequest cr = inProgressCr();
        AppUserPrincipal op = principal(operator);
        executionService.recordExecutionResult(cr.getId(), new RecordExecutionResult(IacApplyResult.SUCCESS));
        recordHealth(cr, HealthCheckItem.IAC_APPLY, HealthResult.HEALTHY);
        recordHealth(cr, HealthCheckItem.HTTP_HEALTH, HealthResult.HEALTHY);
        recordHealth(cr, HealthCheckItem.DB_CONNECTION, HealthResult.HEALTHY);

        ChangeRequest done = changeRequestService.transition(op, cr.getId(), TransitionAction.COMPLETE, null);

        assertThat(done.getStatus()).isEqualTo(ChangeRequestStatus.COMPLETED);
        Execution e = executionRepository.findAll().get(0);
        assertThat(e.isServiceHealthConfirmed()).isTrue();
        assertThat(e.getFinishedAt()).isNotNull();
    }

    @Test
    void iac成功でも必須ヘルスにUNHEALTHYがあればCOMPLETEは409() {
        ChangeRequest cr = inProgressCr();
        AppUserPrincipal op = principal(operator);
        executionService.recordExecutionResult(cr.getId(), new RecordExecutionResult(IacApplyResult.SUCCESS));
        recordHealth(cr, HealthCheckItem.IAC_APPLY, HealthResult.HEALTHY);
        recordHealth(cr, HealthCheckItem.HTTP_HEALTH, HealthResult.HEALTHY);
        recordHealth(cr, HealthCheckItem.DB_CONNECTION, HealthResult.UNHEALTHY);

        assertThatThrownBy(() ->
                changeRequestService.transition(op, cr.getId(), TransitionAction.COMPLETE, null))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThat(changeRequestRepository.findById(cr.getId()).orElseThrow().getStatus())
                .isEqualTo(ChangeRequestStatus.IN_PROGRESS);
    }

    @Test
    void ヘルス正常でもiac未記録ならCOMPLETEは409() {
        ChangeRequest cr = inProgressCr();
        AppUserPrincipal op = principal(operator);
        recordHealth(cr, HealthCheckItem.IAC_APPLY, HealthResult.HEALTHY);
        recordHealth(cr, HealthCheckItem.HTTP_HEALTH, HealthResult.HEALTHY);
        recordHealth(cr, HealthCheckItem.DB_CONNECTION, HealthResult.HEALTHY);

        assertThatThrownBy(() ->
                changeRequestService.transition(op, cr.getId(), TransitionAction.COMPLETE, null))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThat(changeRequestRepository.findById(cr.getId()).orElseThrow().getStatus())
                .isEqualTo(ChangeRequestStatus.IN_PROGRESS);
    }

    @Test
    void FAILからROLLBACKでrollbackが記録される() {
        ChangeRequest cr = inProgressCr();
        AppUserPrincipal op = principal(operator);

        ChangeRequest failed = changeRequestService.transition(op, cr.getId(), TransitionAction.FAIL, null);
        assertThat(failed.getStatus()).isEqualTo(ChangeRequestStatus.FAILED);

        ChangeRequest rolled = changeRequestService.transition(op, cr.getId(),
                TransitionAction.ROLLBACK, new TransitionRequest("手順どおり戻した", null));
        assertThat(rolled.getStatus()).isEqualTo(ChangeRequestStatus.ROLLED_BACK);

        Execution e = executionRepository.findAll().get(0);
        assertThat(e.isRollbackPerformed()).isTrue();
        assertThat(e.getRollbackNote()).isEqualTo("手順どおり戻した");
        assertThat(e.getFinishedAt()).isNotNull();
    }
}
