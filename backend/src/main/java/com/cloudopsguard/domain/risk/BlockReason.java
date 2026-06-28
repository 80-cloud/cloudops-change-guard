package com.cloudopsguard.domain.risk;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ブロック理由の1件（PolicyBlockedException の details 要素＝API レスポンス用）。
 *
 * @param kind              "RISK"（危険なリスクルール）または "POLICY"（BLOCK ポリシー）
 * @param code              ルールコード／ポリシーコード
 * @param message           なぜ危険か／ポリシーのメッセージ
 * @param recommendedAction 推奨対応（RISK のみ・POLICY では null）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BlockReason(String kind, String code, String message, String recommendedAction) {
}
