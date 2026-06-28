package com.cloudopsguard.changerequest;

import com.cloudopsguard.common.exception.IllegalStateTransitionException;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.common.TransitionAction;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.security.AppUserPrincipal;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 不正な状態遷移の拒否（A-9）。
 */
class ChangeRequestTransitionTest extends AbstractIntegrationTest {

    @Autowired
    ChangeRequestService service;

    /** T-4：DRAFT → COMPLETED のような飛び級遷移は 409（許可表にない）で拒否される。 */
    @Test
    void illegalTransitionIsRejected() {
        User requester = createUser("req", Role.REQUESTER);
        User operator = createUser("op", Role.OPERATOR);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);

        AppUserPrincipal actor = principal(operator);
        IllegalStateTransitionException ex = catchThrowableOfType(
                IllegalStateTransitionException.class,
                () -> service.transition(actor, cr.getId(), TransitionAction.COMPLETE, null));

        assertThat(ex).isNotNull();
        assertThat(ex.getCurrentStatus()).isEqualTo("DRAFT");
        // DB 上の状態が変わっていないこと。
        assertThat(changeRequestRepository.findById(cr.getId()).orElseThrow().getStatus())
                .isEqualTo(ChangeRequestStatus.DRAFT);
    }

    /** 補強：正常遷移 submit→review-start→approve が通り、最終状態が APPROVED になる。 */
    @Test
    void happyPathReachesApproved() {
        User requester = createUser("req", Role.REQUESTER);
        User reviewer = createUser("rev", Role.REVIEWER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);

        service.transition(principal(requester), cr.getId(), TransitionAction.SUBMIT, null);
        service.transition(principal(reviewer), cr.getId(), TransitionAction.REVIEW_START, null);
        ChangeRequest approved = service.transition(principal(reviewer), cr.getId(),
                TransitionAction.APPROVE, null);

        assertThat(approved.getStatus()).isEqualTo(ChangeRequestStatus.APPROVED);
        assertThat(approvalRepository.findByChangeRequestIdOrderByDecidedAtAsc(cr.getId())).hasSize(1);
    }

    /** 補強：終端状態（CANCELLED）からは遷移できない。 */
    @Test
    void terminalStateRejectsTransition() {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.CANCELLED);

        assertThatThrownBy(() -> service.transition(principal(requester), cr.getId(),
                TransitionAction.SUBMIT, null))
                .isInstanceOf(IllegalStateTransitionException.class);
    }
}
