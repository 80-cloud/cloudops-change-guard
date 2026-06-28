package com.cloudopsguard.domain.execution.dto;

import com.cloudopsguard.domain.execution.HealthCheckItem;
import com.cloudopsguard.domain.execution.HealthResult;
import jakarta.validation.constraints.NotNull;

/** ヘルスチェック記録リクエスト（OPERATOR）。note は任意。 */
public record CreateHealthCheck(@NotNull HealthCheckItem checkItem,
                                @NotNull HealthResult result,
                                String note) {
}
