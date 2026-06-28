package com.cloudopsguard.domain.changerequest.dto;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.IacType;
import com.cloudopsguard.domain.common.RiskLevel;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 変更申請の詳細 DTO（Entity を直接返さない）。
 * {@code allowedActions} は現在状態・閲覧者のロール/所有から算出した実行可能な遷移一覧（フロントの出し分け補助）。
 */
public record ChangeRequestResponse(
        Long id,
        String title,
        Environment targetEnvironment,
        IacType iacType,
        String targetAwsService,
        String targetResourceName,
        String changeReason,
        String changeSummary,
        String diffText,
        OffsetDateTime scheduledAt,
        String rollbackProcedure,
        ChangeRequestStatus status,
        RiskLevel riskLevel,
        Long requesterId,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<String> allowedActions) {

    public static ChangeRequestResponse from(ChangeRequest cr, List<String> allowedActions) {
        return new ChangeRequestResponse(
                cr.getId(), cr.getTitle(), cr.getTargetEnvironment(), cr.getIacType(),
                cr.getTargetAwsService(), cr.getTargetResourceName(), cr.getChangeReason(),
                cr.getChangeSummary(), cr.getDiffText(), cr.getScheduledAt(), cr.getRollbackProcedure(),
                cr.getStatus(), cr.getRiskLevel(), cr.getRequesterId(), cr.getVersion(),
                cr.getCreatedAt(), cr.getUpdatedAt(), allowedActions);
    }
}
