package com.cloudopsguard.domain.execution.dto;

import com.cloudopsguard.domain.execution.Execution;
import com.cloudopsguard.domain.execution.IacApplyResult;

import java.time.OffsetDateTime;

/** 実施記録の応答（API設計.md §2 詳細・§4）。 */
public record ExecutionResponse(Long id, Long changeRequestId, Long operatorId,
                                IacApplyResult iacApplyResult, boolean serviceHealthConfirmed,
                                OffsetDateTime startedAt, OffsetDateTime finishedAt,
                                boolean rollbackPerformed, String rollbackNote,
                                String applyRunUrl, String planSourceRef) {

    public static ExecutionResponse from(Execution e) {
        return new ExecutionResponse(e.getId(), e.getChangeRequestId(), e.getOperatorId(),
                e.getIacApplyResult(), e.isServiceHealthConfirmed(), e.getStartedAt(),
                e.getFinishedAt(), e.isRollbackPerformed(), e.getRollbackNote(),
                e.getApplyRunUrl(), e.getPlanSourceRef());
    }
}
