package com.cloudopsguard.domain.execution.dto;

import com.cloudopsguard.domain.execution.HealthCheckItem;
import com.cloudopsguard.domain.execution.HealthResult;
import jakarta.validation.constraints.NotNull;

/**
 * ヘルスチェック記録リクエスト（OPERATOR）。note は任意。
 * monitoringRef を指定すると MonitoringStatusProvider が result を解決する（未指定＝従来どおり手入力 result を使う）。
 */
public record CreateHealthCheck(@NotNull HealthCheckItem checkItem,
                                @NotNull HealthResult result,
                                String note,
                                String monitoringRef) {

    /** 後方互換：monitoringRef 省略時は null（既存呼び出し・テストをそのまま通す）。 */
    public CreateHealthCheck(HealthCheckItem checkItem, HealthResult result, String note) {
        this(checkItem, result, note, null);
    }
}
