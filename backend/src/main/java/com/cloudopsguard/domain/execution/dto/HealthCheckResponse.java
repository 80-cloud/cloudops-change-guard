package com.cloudopsguard.domain.execution.dto;

import com.cloudopsguard.domain.execution.HealthCheckItem;
import com.cloudopsguard.domain.execution.HealthResult;
import com.cloudopsguard.domain.execution.PostExecutionHealthCheck;

import java.time.OffsetDateTime;

/** 実施後ヘルスチェック1件の応答（API設計.md §4）。 */
public record HealthCheckResponse(Long id, HealthCheckItem checkItem, HealthResult result,
                                  String note, Long recordedBy, OffsetDateTime recordedAt) {

    public static HealthCheckResponse from(PostExecutionHealthCheck h) {
        return new HealthCheckResponse(h.getId(), h.getCheckItem(), h.getResult(),
                h.getNote(), h.getRecordedBy(), h.getRecordedAt());
    }
}
