package com.cloudopsguard.notification;

import com.cloudopsguard.common.exception.PolicyBlockedException;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.common.TransitionAction;
import com.cloudopsguard.domain.notification.NotificationEvent;
import com.cloudopsguard.domain.notification.NotificationPort;
import com.cloudopsguard.domain.notification.NotificationType;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 通知配線（F-NOTIFY-01）。主要遷移で関係者へ通知が飛び、BLOCK 等の失敗時は飛ばないことを Port モックで検証。
 */
class NotificationDispatchTest extends AbstractIntegrationTest {

    @MockBean
    private NotificationPort notificationPort;

    @Autowired
    private ChangeRequestService service;

    @Test
    void submitでREVIEWER宛に査閲依頼通知が1件送られる() {
        User requester = createUser("reqN", Role.REQUESTER);
        User reviewer = createUser("revN", Role.REVIEWER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);

        service.transition(principal(requester), cr.getId(), TransitionAction.SUBMIT, null);

        ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationPort, times(1)).send(cap.capture());
        NotificationEvent ev = cap.getValue();
        assertThat(ev.type()).isEqualTo(NotificationType.SUBMITTED_FOR_REVIEW);
        assertThat(ev.recipients()).contains(reviewer.getEmail());
    }

    @Test
    void BLOCKされた本番submitでは通知が送られない() {
        User requester = createUser("reqN", Role.REQUESTER);
        createUser("revN", Role.REVIEWER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);
        cr.setTargetEnvironment(Environment.PRODUCTION);
        cr.setDiffText("# aws_db_instance.main will be destroyed");
        changeRequestRepository.save(cr);

        catchThrowableOfType(PolicyBlockedException.class,
                () -> service.transition(principal(requester), cr.getId(), TransitionAction.SUBMIT, null));

        verifyNoInteractions(notificationPort);
    }
}
