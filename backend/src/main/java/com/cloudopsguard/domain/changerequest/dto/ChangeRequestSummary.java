package com.cloudopsguard.domain.changerequest.dto;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.IacType;
import com.cloudopsguard.domain.common.RiskLevel;

import java.time.OffsetDateTime;

/** 一覧用の軽量 DTO（本文 TEXT を含めない）。 */
public record ChangeRequestSummary(
        Long id,
        String title,
        Environment targetEnvironment,
        IacType iacType,
        ChangeRequestStatus status,
        RiskLevel riskLevel,
        Long requesterId,
        OffsetDateTime scheduledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ChangeRequestSummary from(ChangeRequest cr) {
        return new ChangeRequestSummary(
                cr.getId(), cr.getTitle(), cr.getTargetEnvironment(), cr.getIacType(),
                cr.getStatus(), cr.getRiskLevel(), cr.getRequesterId(),
                cr.getScheduledAt(), cr.getCreatedAt(), cr.getUpdatedAt());
    }
}
