package com.cloudopsguard.domain.audit.dto;

import com.cloudopsguard.domain.audit.AuditLog;

import java.time.OffsetDateTime;

/** 監査ログの応答 DTO（Entity を直接返さない）。 */
public record AuditLogResponse(
        Long id,
        Long changeRequestId,
        Long actorId,
        String actionType,
        String beforeStatus,
        String afterStatus,
        String comment,
        String summary,
        OffsetDateTime createdAt) {

    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getChangeRequestId(),
                a.getActorId(),
                a.getActionType().name(),
                a.getBeforeStatus(),
                a.getAfterStatus(),
                a.getComment(),
                a.getSummary(),
                a.getCreatedAt());
    }
}
