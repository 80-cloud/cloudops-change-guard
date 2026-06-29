package com.cloudopsguard.domain.notification;

import java.util.List;

/** 通知1件（宛先はメールアドレス）。送信実装に依存しない不変イベント。 */
public record NotificationEvent(
        NotificationType type,
        Long changeRequestId,
        String title,
        List<String> recipients,
        String message) {
}
