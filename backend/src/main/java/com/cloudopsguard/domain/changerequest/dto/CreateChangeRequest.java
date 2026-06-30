package com.cloudopsguard.domain.changerequest.dto;

import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.IacType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 変更申請の作成（DRAFT）。最低限 title / target_environment / iac_type を必須にする（B6）。
 * 残りの本文は下書き段階では任意で、SUBMIT 時に状態機械が必須充足を検証する。
 *
 * <p>planSourceRef は実 plan の取込参照（例：S3 オブジェクトキー）。指定時は IaCChangeProvider が
 * 差分テキストを解決する（未指定＝従来どおり手貼り diffText を使う）。
 */
public record CreateChangeRequest(
        @NotNull @Size(min = 1, max = 200) String title,
        @NotNull Environment targetEnvironment,
        @NotNull IacType iacType,
        @Size(max = 50) String targetAwsService,
        @Size(max = 200) String targetResourceName,
        String changeReason,
        String changeSummary,
        String diffText,
        OffsetDateTime scheduledAt,
        String rollbackProcedure,
        String planSourceRef) {

    /** 後方互換：planSourceRef 省略時は null（既存呼び出し・テストをそのまま通す）。 */
    public CreateChangeRequest(String title, Environment targetEnvironment, IacType iacType,
                               String targetAwsService, String targetResourceName, String changeReason,
                               String changeSummary, String diffText, OffsetDateTime scheduledAt,
                               String rollbackProcedure) {
        this(title, targetEnvironment, iacType, targetAwsService, targetResourceName, changeReason,
                changeSummary, diffText, scheduledAt, rollbackProcedure, null);
    }
}
