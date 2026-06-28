package com.cloudopsguard.changerequest;

import com.cloudopsguard.common.exception.ForbiddenException;
import com.cloudopsguard.common.exception.IllegalStateTransitionException;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestStateMachine;
import com.cloudopsguard.domain.changerequest.ChangeRequestStateMachine.TransitionContext;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.common.TransitionAction;
import com.cloudopsguard.security.AppUserPrincipal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 状態機械の単体テスト（許可遷移・不正遷移・ロール/所有/自己承認ガードの網羅）。Spring 文脈は使わない。
 */
class ChangeRequestStateMachineTest {

    private final ChangeRequestStateMachine sm = new ChangeRequestStateMachine();

    private ChangeRequest cr(ChangeRequestStatus status, long requesterId) {
        ChangeRequest cr = new ChangeRequest();
        cr.setId(1L);
        cr.setStatus(status);
        cr.setRequesterId(requesterId);
        // 必須項目を充足（SUBMIT ガード用）。
        cr.setTitle("t");
        cr.setTargetEnvironment(com.cloudopsguard.domain.common.Environment.STAGING);
        cr.setIacType(com.cloudopsguard.domain.common.IacType.TERRAFORM);
        cr.setTargetAwsService("RDS");
        cr.setTargetResourceName("db");
        cr.setChangeReason("r");
        cr.setChangeSummary("s");
        cr.setDiffText("d");
        return cr;
    }

    private AppUserPrincipal user(long id, Role role) {
        return new AppUserPrincipal(id, "u" + id, role);
    }

    @Test
    void ownerCanSubmitDraft() {
        ChangeRequest c = cr(ChangeRequestStatus.DRAFT, 10L);
        sm.transition(c, TransitionAction.SUBMIT, user(10L, Role.REQUESTER), TransitionContext.empty());
        assertThat(c.getStatus()).isEqualTo(ChangeRequestStatus.SUBMITTED);
    }

    @Test
    void allowedActionsForDraftOwner() {
        ChangeRequest c = cr(ChangeRequestStatus.DRAFT, 10L);
        assertThat(sm.allowedActions(c, user(10L, Role.REQUESTER)))
                .containsExactlyInAnyOrder("submit", "cancel");
    }

    @Test
    void skipLevelTransitionRejected() {
        ChangeRequest c = cr(ChangeRequestStatus.DRAFT, 10L);
        assertThatThrownBy(() -> sm.transition(c, TransitionAction.COMPLETE,
                user(99L, Role.OPERATOR), TransitionContext.empty()))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void wrongRoleRejectedWith403() {
        ChangeRequest c = cr(ChangeRequestStatus.DRAFT, 10L);
        // submit は REQUESTER 専用。REVIEWER は不可。
        assertThatThrownBy(() -> sm.transition(c, TransitionAction.SUBMIT,
                user(20L, Role.REVIEWER), TransitionContext.empty()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void selfApprovalRejected() {
        ChangeRequest c = cr(ChangeRequestStatus.UNDER_REVIEW, 10L);
        assertThatThrownBy(() -> sm.transition(c, TransitionAction.APPROVE,
                user(10L, Role.REVIEWER), TransitionContext.empty()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void otherReviewerCanApprove() {
        ChangeRequest c = cr(ChangeRequestStatus.UNDER_REVIEW, 10L);
        sm.transition(c, TransitionAction.APPROVE, user(20L, Role.REVIEWER), TransitionContext.empty());
        assertThat(c.getStatus()).isEqualTo(ChangeRequestStatus.APPROVED);
    }

    @Test
    void terminalStateHasNoAllowedActions() {
        ChangeRequest c = cr(ChangeRequestStatus.COMPLETED, 10L);
        assertThat(sm.allowedActions(c, user(20L, Role.OPERATOR))).isEmpty();
        assertThatThrownBy(() -> sm.transition(c, TransitionAction.CANCEL,
                user(20L, Role.ADMIN), TransitionContext.empty()))
                .isInstanceOf(IllegalStateTransitionException.class);
    }
}
