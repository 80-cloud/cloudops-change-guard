package com.cloudopsguard.domain.notification;

/** 通知の送信口（Port）。実装を差し替えて実送信（メール/チャット等）へ繋ぐ。 */
public interface NotificationPort {
    void send(NotificationEvent event);
}
