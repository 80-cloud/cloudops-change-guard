package com.cloudopsguard.domain.approval.dto;

import com.cloudopsguard.domain.approval.Approval;
import com.cloudopsguard.domain.common.Decision;

import java.time.OffsetDateTime;

/** 承認・却下・差し戻しの記録の応答 DTO（承認履歴・SCR-04）。 */
public record ApprovalResponse(Long id, Long changeRequestId, Long reviewerId,
                               Decision decision, String comment, OffsetDateTime decidedAt) {

    public static ApprovalResponse from(Approval a) {
        return new ApprovalResponse(a.getId(), a.getChangeRequestId(), a.getReviewerId(),
                a.getDecision(), a.getComment(), a.getDecidedAt());
    }
}
