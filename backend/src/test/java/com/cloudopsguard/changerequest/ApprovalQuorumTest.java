package com.cloudopsguard.changerequest;

import com.cloudopsguard.common.exception.ValidationException;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.common.TransitionAction;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 承認段数（approval-flow.json マトリクス）の強制（Increment 5・受入 A-6）。
 * development×CRITICAL は「異なる REVIEWER 2名＋ロールバック手順」が要件。
 */
class ApprovalQuorumTest extends AbstractIntegrationTest {

    @Autowired
    private ChangeRequestService service;

    /** development×CRITICAL（RDS削除・dev はブロックされない）。rollbackProcedure の有無で切替。 */
    private ChangeRequest criticalDevChange(User requester, String rollbackProcedure) {
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);
        cr.setTargetEnvironment(Environment.DEVELOPMENT);
        cr.setDiffText("# aws_db_instance.main will be destroyed");
        cr.setRollbackProcedure(rollbackProcedure);
        return changeRequestRepository.save(cr);
    }

    @Test
    void criticalは異なる2名の承認でAPPROVEDになる() {
        User requester = createUser("req", Role.REQUESTER);
        User reviewer1 = createUser("rev1", Role.REVIEWER);
        User reviewer2 = createUser("rev2", Role.REVIEWER);
        ChangeRequest cr = criticalDevChange(requester, "スナップショットから復元する");
        Long id = cr.getId();

        service.transition(principal(requester), id, TransitionAction.SUBMIT, null);
        service.transition(principal(reviewer1), id, TransitionAction.REVIEW_START, null);

        // 1人目の承認：定足数(2)未達なので UNDER_REVIEW を維持。
        ChangeRequest afterFirst = service.transition(principal(reviewer1), id,
                TransitionAction.APPROVE, null);
        assertThat(afterFirst.getStatus()).isEqualTo(ChangeRequestStatus.UNDER_REVIEW);

        // 2人目（別人）の承認：定足数に達し APPROVED へ。
        ChangeRequest afterSecond = service.transition(principal(reviewer2), id,
                TransitionAction.APPROVE, null);
        assertThat(afterSecond.getStatus()).isEqualTo(ChangeRequestStatus.APPROVED);

        assertThat(approvalRepository.findByChangeRequestIdOrderByDecidedAtAsc(id)).hasSize(2);
    }

    @Test
    void criticalでロールバック手順が無いと承認できない() {
        User requester = createUser("req", Role.REQUESTER);
        User reviewer1 = createUser("rev1", Role.REVIEWER);
        ChangeRequest cr = criticalDevChange(requester, null);   // ロールバック手順なし
        Long id = cr.getId();

        service.transition(principal(requester), id, TransitionAction.SUBMIT, null);
        service.transition(principal(reviewer1), id, TransitionAction.REVIEW_START, null);

        ValidationException ex = catchThrowableOfType(ValidationException.class,
                () -> service.transition(principal(reviewer1), id, TransitionAction.APPROVE, null));

        assertThat(ex).isNotNull();
        // 承認前提を満たさないため、票は記録されず UNDER_REVIEW のまま。
        assertThat(changeRequestRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(ChangeRequestStatus.UNDER_REVIEW);
        assertThat(approvalRepository.findByChangeRequestIdOrderByDecidedAtAsc(id)).isEmpty();
    }
}
