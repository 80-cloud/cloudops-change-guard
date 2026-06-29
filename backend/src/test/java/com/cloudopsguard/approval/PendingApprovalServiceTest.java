package com.cloudopsguard.approval;

import com.cloudopsguard.domain.approval.PendingApprovalService;
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
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 承認待ち一覧（SCR-05）の絞り込みの検証。
 * listFor が「UNDER_REVIEW かつ 自分が申請者でない かつ 自分が未投票」のみ返すことを、
 * UNDER_REVIEW を維持したまま実証する。承認段数のロジックは ApprovalQuorumTest が担保するため、
 * ここは「未投票のみ」の絞り込み（この経路だけ自動テストが無かった）に焦点を当てる。
 */
class PendingApprovalServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ChangeRequestService changeRequestService;

    @Autowired
    private PendingApprovalService pendingApprovalService;

    private ChangeRequest criticalDevChange(User requester) {
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);
        cr.setTargetEnvironment(Environment.DEVELOPMENT);
        cr.setDiffText("# aws_db_instance.main will be destroyed");
        cr.setRollbackProcedure("スナップショットから復元する");
        return changeRequestRepository.save(cr);
    }

    private List<Long> pendingIdsFor(User reviewer) {
        return pendingApprovalService.listFor(principal(reviewer), PageRequest.of(0, 20))
                .map(ChangeRequest::getId).getContent();
    }

    @Test
    void 投票済みレビュー者からは消え未投票レビュー者には残る() {
        User requester = createUser("req", Role.REQUESTER);
        User reviewer1 = createUser("rev1", Role.REVIEWER);
        User reviewer2 = createUser("rev2", Role.REVIEWER);
        Long id = criticalDevChange(requester).getId();

        changeRequestService.transition(principal(requester), id, TransitionAction.SUBMIT, null);
        changeRequestService.transition(principal(reviewer1), id, TransitionAction.REVIEW_START, null);

        assertThat(pendingIdsFor(reviewer1)).contains(id);
        assertThat(pendingIdsFor(reviewer2)).contains(id);

        ChangeRequest afterFirst = changeRequestService.transition(principal(reviewer1), id,
                TransitionAction.APPROVE, null);
        assertThat(afterFirst.getStatus()).isEqualTo(ChangeRequestStatus.UNDER_REVIEW);

        assertThat(pendingIdsFor(reviewer1)).doesNotContain(id);
        assertThat(pendingIdsFor(reviewer2)).contains(id);
    }

    @Test
    void UNDER_REVIEW以外は承認待ちに出ない() {
        User requester = createUser("req", Role.REQUESTER);
        User reviewer = createUser("rev1", Role.REVIEWER);
        ChangeRequest draft = createChangeRequest(requester, ChangeRequestStatus.DRAFT);

        assertThat(pendingIdsFor(reviewer)).doesNotContain(draft.getId());
    }

    @Test
    void 自分が申請したものは承認待ちに出ない() {
        User reviewerAsRequester = createUser("rev1", Role.REVIEWER);
        ChangeRequest own = createChangeRequest(reviewerAsRequester, ChangeRequestStatus.UNDER_REVIEW);

        assertThat(pendingIdsFor(reviewerAsRequester)).doesNotContain(own.getId());
    }
}
