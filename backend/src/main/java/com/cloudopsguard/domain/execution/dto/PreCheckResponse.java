package com.cloudopsguard.domain.execution.dto;

import com.cloudopsguard.domain.execution.CheckType;
import com.cloudopsguard.domain.execution.PreExecutionCheck;

import java.time.OffsetDateTime;

/** 実施前チェック1項目の応答（API設計.md §4）。 */
public record PreCheckResponse(Long id, CheckType checkType, boolean required,
                               boolean completed, Long completedBy, OffsetDateTime completedAt) {

    public static PreCheckResponse from(PreExecutionCheck c) {
        return new PreCheckResponse(c.getId(), c.getCheckType(), c.isRequired(),
                c.isCompleted(), c.getCompletedBy(), c.getCompletedAt());
    }
}
