package com.cloudopsguard.changerequest;

import com.cloudopsguard.common.exception.PolicyBlockedException;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.common.TransitionAction;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * SUBMIT 時のリスク判定＋ BLOCK 連動（Increment 4・受入 A-4/A-5）。
 * 危険な本番変更は BLOCK で提出を拒否し、状態と判定をロールバックする。無害な変更は提出が通り判定が残る。
 */
class SubmitRiskGuardTest extends AbstractIntegrationTest {

    @Autowired
    private ChangeRequestService service;

    private ChangeRequest prodRdsDelete(User requester) {
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);
        cr.setTargetEnvironment(Environment.PRODUCTION);
        cr.setDiffText("# aws_db_instance.main will be destroyed");
        return changeRequestRepository.save(cr);
    }

    @Test
    void 危険な本番変更のsubmitはBLOCKで拒否されDRAFTのまま() {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = prodRdsDelete(requester);

        PolicyBlockedException ex = catchThrowableOfType(PolicyBlockedException.class,
                () -> service.transition(principal(requester), cr.getId(), TransitionAction.SUBMIT, null));

        assertThat(ex).isNotNull();
        assertThat(ex.getDetails()).isNotEmpty();

        // 遷移はロールバック：DRAFT のまま、判定行も残らない（原子性・B5）。
        assertThat(changeRequestRepository.findById(cr.getId()).orElseThrow().getStatus())
                .isEqualTo(ChangeRequestStatus.DRAFT);
        assertThat(riskAssessmentRepository.findAll()).isEmpty();
    }

    @Test
    void 無害な変更のsubmitは通りSUBMITTEDになり判定が残る() {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);

        ChangeRequest submitted = service.transition(principal(requester), cr.getId(),
                TransitionAction.SUBMIT, null);

        assertThat(submitted.getStatus()).isEqualTo(ChangeRequestStatus.SUBMITTED);
        assertThat(submitted.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(riskAssessmentRepository.findAll()).hasSize(1);
    }
}
