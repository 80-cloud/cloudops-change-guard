package com.cloudopsguard.domain.changerequest.dto;

import com.cloudopsguard.domain.execution.dto.ExecutionResponse;
import com.cloudopsguard.domain.execution.dto.HealthCheckResponse;
import com.cloudopsguard.domain.execution.dto.PreCheckResponse;

import java.util.List;

/**
 * 変更申請の集約詳細（GET /change-requests/{id}）。基本情報に実施前チェック・実施後ヘルスチェック・
 * 最新の実施記録を同梱する（API設計.md §2）。承認履歴・監査ログは専用エンドポイントで取得する。
 * execution は未実施なら null。
 */
public record ChangeRequestDetailResponse(
        ChangeRequestResponse changeRequest,
        List<PreCheckResponse> preChecks,
        List<HealthCheckResponse> healthChecks,
        ExecutionResponse execution) {
}
