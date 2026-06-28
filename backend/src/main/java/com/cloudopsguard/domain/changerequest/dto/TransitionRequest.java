package com.cloudopsguard.domain.changerequest.dto;

import java.time.OffsetDateTime;

/**
 * 状態遷移エンドポイント共通の任意ボディ。
 * 差し戻し/却下/取消の理由コメント、schedule の実施予定日時を運ぶ（不要な遷移では null）。
 */
public record TransitionRequest(String comment, OffsetDateTime scheduledAt) {
}
