package com.cloudopsguard.domain.execution.dto;

import com.cloudopsguard.domain.execution.IacApplyResult;
import jakarta.validation.constraints.NotNull;

/** 実行結果記録リクエスト（OPERATOR・決定A）。IaC 適用結果を execution に設定する。 */
public record RecordExecutionResult(@NotNull IacApplyResult iacApplyResult) {
}
