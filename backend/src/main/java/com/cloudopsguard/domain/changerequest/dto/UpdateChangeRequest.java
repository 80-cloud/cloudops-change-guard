package com.cloudopsguard.domain.changerequest.dto;

import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.IacType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 変更申請の編集（DRAFT / RETURNED の所有者のみ）。承認済み以降は編集不可（受入 A-3・Service で検証）。
 */
public record UpdateChangeRequest(
        @NotNull @Size(min = 1, max = 200) String title,
        @NotNull Environment targetEnvironment,
        @NotNull IacType iacType,
        @Size(max = 50) String targetAwsService,
        @Size(max = 200) String targetResourceName,
        String changeReason,
        String changeSummary,
        String diffText,
        OffsetDateTime scheduledAt,
        String rollbackProcedure) {
}
