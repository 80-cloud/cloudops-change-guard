package com.cloudopsguard.domain.execution;

import java.util.Optional;

/**
 * EC2/SSM インスタンス状態の取り込み口（Port）。手入力のヘルス結果に代えて、SSM の管理対象インスタンス情報
 * （DescribeInstanceInformation 等）から実効の HealthResult を取得する。実装は {@code @Profile} で差し替える
 * （既定＝Mock／aws＝実 Adapter）。Service はこの Port にのみ依存し、SDK 型を直接扱わない
 * （docs/AWS・IaC連携方針.md §1）。
 */
public interface InstanceStatusProvider {

    /** インスタンス参照（例：インスタンス ID）から実効ヘルスを取得する。取得できなければ empty。 */
    Optional<HealthResult> fetchInstanceHealth(String instanceRef);

    /**
     * 実効の結果を決める。instanceRef が無ければ手入力 manualResult をそのまま使う（＝既定挙動の維持）。
     * instanceRef があり取得できればそれを、できなければ manualResult を返す。
     */
    default HealthResult resolveResult(String instanceRef, HealthResult manualResult) {
        if (instanceRef == null || instanceRef.isBlank()) {
            return manualResult;
        }
        return fetchInstanceHealth(instanceRef).orElse(manualResult);
    }
}
