package com.cloudopsguard.domain.execution.dto;

import com.cloudopsguard.domain.execution.IacApplyResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 実行結果記録リクエスト（OPERATOR・決定A）。IaC 適用結果に加え、外部で実行された apply の
 * 証跡（Run URL・取り込んだ plan の参照）を任意で記録する（A-2b）。Backend は apply を実行しない。
 */
public record RecordExecutionResult(
        @NotNull IacApplyResult iacApplyResult,
        @Size(max = 2048) String applyRunUrl,
        @Size(max = 2048) String planSourceRef) {

    /** 後方互換：適用結果のみを記録する既存呼び出し（証跡なし）。 */
    public RecordExecutionResult(IacApplyResult iacApplyResult) {
        this(iacApplyResult, null, null);
    }
}
