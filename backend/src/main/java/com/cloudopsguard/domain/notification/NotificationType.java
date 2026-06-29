package com.cloudopsguard.domain.notification;

/** 通知の種類（主要な状態遷移に対応）。 */
public enum NotificationType {
    SUBMITTED_FOR_REVIEW,
    APPROVED_FOR_EXECUTION,
    REJECTED,
    RETURNED,
    EXECUTION_FAILED,
    EXECUTION_COMPLETED
}
