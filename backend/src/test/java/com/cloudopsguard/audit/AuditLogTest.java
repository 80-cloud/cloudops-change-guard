package com.cloudopsguard.audit;

import com.cloudopsguard.domain.audit.AuditLog;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.comment.CommentService;
import com.cloudopsguard.domain.common.AuditAction;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.common.TransitionAction;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * すべての遷移・コメントで監査ログが記録されること（A-8）。
 */
class AuditLogTest extends AbstractIntegrationTest {

    @Autowired
    ChangeRequestService changeRequestService;
    @Autowired
    CommentService commentService;

    private boolean hasAction(Long crId, AuditAction action) {
        List<AuditLog> logs = auditLogRepository.findByChangeRequestIdOrderByCreatedAtDesc(crId);
        return logs.stream().anyMatch(l -> l.getActionType() == action);
    }

    /** T-5：提出時に監査ログ(SUBMIT)が作成される。 */
    @Test
    void submitRecordsAudit() {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);

        changeRequestService.transition(principal(requester), cr.getId(), TransitionAction.SUBMIT, null);

        assertThat(hasAction(cr.getId(), AuditAction.SUBMIT)).isTrue();
    }

    /** T-6：承認時に監査ログ(APPROVE)が作成される。 */
    @Test
    void approveRecordsAudit() {
        User requester = createUser("req", Role.REQUESTER);
        User reviewer = createUser("rev", Role.REVIEWER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.UNDER_REVIEW);

        changeRequestService.transition(principal(reviewer), cr.getId(), TransitionAction.APPROVE, null);

        assertThat(hasAction(cr.getId(), AuditAction.APPROVE)).isTrue();
    }

    /** T-7：コメント投稿時に監査ログ(COMMENT)が作成される。 */
    @Test
    void commentRecordsAudit() {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);

        commentService.create(principal(requester), cr.getId(), "バックアップ確認をお願いします");

        assertThat(hasAction(cr.getId(), AuditAction.COMMENT)).isTrue();
    }
}
