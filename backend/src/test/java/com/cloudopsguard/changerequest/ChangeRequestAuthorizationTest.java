package com.cloudopsguard.changerequest;

import com.cloudopsguard.common.exception.ForbiddenException;
import com.cloudopsguard.common.exception.IllegalStateTransitionException;
import com.cloudopsguard.common.exception.NotFoundException;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.changerequest.dto.TransitionRequest;
import com.cloudopsguard.domain.changerequest.dto.UpdateChangeRequest;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.IacType;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.common.TransitionAction;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.security.AppUserPrincipal;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 認可と所有者検証の受入テスト（A-1 / A-2 / A-3）。
 */
class ChangeRequestAuthorizationTest extends AbstractIntegrationTest {

    @Autowired
    ChangeRequestService service;

    private UpdateChangeRequest validUpdate() {
        return new UpdateChangeRequest("更新後タイトル", Environment.STAGING, IacType.TERRAFORM,
                "RDS", "app-db", "理由", "概要", "diff", null, null);
    }

    /** T-1：REQUESTER は他人の申請を編集できない（存在秘匿で 404）。 */
    @Test
    void requesterCannotEditOthersRequest() {
        User owner = createUser("owner", Role.REQUESTER);
        User other = createUser("other", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(owner, ChangeRequestStatus.DRAFT);

        assertThatThrownBy(() -> service.update(principal(other), cr.getId(), validUpdate()))
                .isInstanceOf(NotFoundException.class);
    }

    /** T-2：自分が申請した変更は承認できない（自己承認禁止）。 */
    @Test
    void cannotApproveOwnRequest() {
        // 同一人物が申請者かつレビュー者であるケース（自己承認）を直接構成する。
        User self = createUser("selfreviewer", Role.REVIEWER);
        ChangeRequest cr = createChangeRequest(self, ChangeRequestStatus.UNDER_REVIEW);

        AppUserPrincipal actor = principal(self);
        assertThatThrownBy(() -> service.transition(actor, cr.getId(), TransitionAction.APPROVE, null))
                .isInstanceOf(ForbiddenException.class);
    }

    /** T-3：承認済み（APPROVED 以降）の申請は申請者でも編集できない。 */
    @Test
    void cannotEditApprovedRequest() {
        User owner = createUser("owner", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(owner, ChangeRequestStatus.APPROVED);

        assertThatThrownBy(() -> service.update(principal(owner), cr.getId(), validUpdate()))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    /** 補強：差し戻しはコメント必須（ガード）。 */
    @Test
    void returnRequiresComment() {
        User requester = createUser("req", Role.REQUESTER);
        User reviewer = createUser("rev", Role.REVIEWER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.UNDER_REVIEW);

        AppUserPrincipal actor = principal(reviewer);
        assertThatThrownBy(() -> service.transition(actor, cr.getId(),
                TransitionAction.RETURN_, new TransitionRequest(" ", null)))
                .isInstanceOf(com.cloudopsguard.common.exception.ValidationException.class);
    }
}
