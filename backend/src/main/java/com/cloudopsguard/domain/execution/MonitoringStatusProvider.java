package com.cloudopsguard.domain.execution;

import java.util.Optional;

/**
 * 監視状態の取り込み口（Port）。手入力のヘルス結果に代えて、外部の監視（CloudWatch アラーム等）から
 * 実効の HealthResult を取得する。実装は {@code @Profile} で差し替える（既定＝Mock／aws＝実 Adapter）。
 * Service はこの Port にのみ依存し、SDK 型を直接扱わない（docs/AWS・IaC連携方針.md §1）。
 */
public interface MonitoringStatusProvider {

    /** 監視参照（例：アラーム名やプレフィックス）から実効ヘルスを取得する。取得できなければ empty。 */
    Optional<HealthResult> fetchAlarmHealth(String monitoringRef);

    /**
     * 実効の結果を決める。monitoringRef が無ければ手入力 manualResult をそのまま使う（＝既定挙動の維持）。
     * monitoringRef があり取得できればそれを、できなければ manualResult を返す。
     */
    default HealthResult resolveResult(String monitoringRef, HealthResult manualResult) {
        if (monitoringRef == null || monitoringRef.isBlank()) {
            return manualResult;
        }
        return fetchAlarmHealth(monitoringRef).orElse(manualResult);
    }
}
