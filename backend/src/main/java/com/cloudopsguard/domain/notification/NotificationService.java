package com.cloudopsguard.domain.notification;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 主要な状態遷移で関係者へ通知する（F-NOTIFY-01）。宛先解決＋整形を担い、送信は {@link NotificationPort} へ委譲。
 * 外部送信の失敗で本処理（遷移・記録）を止めないよう、ここで例外を握り潰す（堅牢化方針）。
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationPort port;
    private final UserRepository userRepository;

    public NotificationService(NotificationPort port, UserRepository userRepository) {
        this.port = port;
        this.userRepository = userRepository;
    }

    public void notify(NotificationType type, ChangeRequest cr) {
        try {
            List<String> recipients = resolveRecipients(type, cr);
            if (recipients.isEmpty()) {
                return;
            }
            port.send(new NotificationEvent(type, cr.getId(), cr.getTitle(), recipients, messageFor(type)));
        } catch (Exception e) {
            log.warn("通知の送信に失敗しました（本処理は継続）: type={} cr=#{}", type, cr.getId(), e);
        }
    }

    private List<String> resolveRecipients(NotificationType type, ChangeRequest cr) {
        return switch (type) {
            case SUBMITTED_FOR_REVIEW -> emailsOfRole(Role.REVIEWER);
            case APPROVED_FOR_EXECUTION -> emailsOfRole(Role.OPERATOR);
            case REJECTED, RETURNED, EXECUTION_FAILED, EXECUTION_COMPLETED -> requesterEmail(cr);
        };
    }

    private List<String> emailsOfRole(Role role) {
        return userRepository.findByRole(role).stream().map(User::getEmail).toList();
    }

    private List<String> requesterEmail(ChangeRequest cr) {
        if (cr.getRequesterId() == null) {
            return List.of();
        }
        return userRepository.findById(cr.getRequesterId())
                .map(u -> List.of(u.getEmail()))
                .orElseGet(List::of);
    }

    private String messageFor(NotificationType type) {
        return switch (type) {
            case SUBMITTED_FOR_REVIEW -> "新しい変更申請が査閲をお待ちしています。";
            case APPROVED_FOR_EXECUTION -> "変更申請が承認されました。実施をお願いします。";
            case REJECTED -> "変更申請が却下されました。";
            case RETURNED -> "変更申請が差し戻されました。内容をご確認ください。";
            case EXECUTION_FAILED -> "変更の実施が失敗として記録されました。";
            case EXECUTION_COMPLETED -> "変更の実施が完了しました。";
        };
    }
}
