package com.cloudopsguard.domain.risk;

import java.util.Optional;

/**
 * IaC 変更の取り込み口（Port）。手貼りの差分テキストに代えて、外部ソース（実 plan 等）から
 * 差分テキストを取得する。実装は {@code @Profile} で差し替える（既定＝Mock／aws＝実 Adapter）。
 * Controller/Service はこの Port にのみ依存し、SDK 型を直接扱わない（docs/AWS・IaC連携方針.md §1）。
 */
public interface IaCChangeProvider {

    /** 参照（例：S3 オブジェクトキー）から差分テキストを取得する。取得できなければ empty。 */
    Optional<String> fetchPlanText(String sourceRef);

    /**
     * 実効の差分テキストを決める。sourceRef が無ければ手貼り fallback をそのまま使う（＝既定挙動の維持）。
     * sourceRef があり取得できればそれを、できなければ fallback を返す。
     */
    default String resolveDiffText(String sourceRef, String fallbackDiffText) {
        if (sourceRef == null || sourceRef.isBlank()) {
            return fallbackDiffText;
        }
        return fetchPlanText(sourceRef).orElse(fallbackDiffText);
    }
}
