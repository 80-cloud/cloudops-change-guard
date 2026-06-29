package com.cloudopsguard.domain.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 既定アダプタ：実送信せずログ出力する（dev・MVP）。
 * 将来、実送信アダプタ（メール/チャット）へ差し替える。送信先の資格情報は環境変数で渡す前提（コード直書きしない）。
 */
@Component
public class MockNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationAdapter.class);

    @Override
    public void send(NotificationEvent event) {
        log.info("[通知] type={} cr=#{} title=\"{}\" 宛先={} 本文={}",
                event.type(), event.changeRequestId(), event.title(),
                event.recipients(), event.message());
    }
}
