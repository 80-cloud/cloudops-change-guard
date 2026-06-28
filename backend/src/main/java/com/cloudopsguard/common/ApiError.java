package com.cloudopsguard.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * エラーレスポンス共通フォーマット（API設計.md §0 / 状態遷移設計.md §5）。
 *
 * <pre>
 * {
 *   "error": {
 *     "code": "ILLEGAL_STATE_TRANSITION",
 *     "message": "...",            // ユーザー向け
 *     "reason": "...",             // 詳細理由（任意）
 *     "currentStatus": "DRAFT",    // 状態遷移エラー時（任意）
 *     "allowedActions": ["submit"],// その利用者が実行可能な遷移（任意）
 *     "details": [ ... ]           // ポリシー違反など（任意）
 *   }
 * }
 * </pre>
 *
 * <p>スタックトレース・SQL・内部クラス名は載せない（内部情報を漏らさない）。
 * 値が無いフィールドは JSON に出さない（NON_NULL）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(ErrorBody error) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(
            String code,
            String message,
            String reason,
            String currentStatus,
            List<String> allowedActions,
            List<Object> details) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(new ErrorBody(code, message, null, null, null, null));
    }

    public static ApiError of(String code, String message, String reason) {
        return new ApiError(new ErrorBody(code, message, reason, null, null, null));
    }

    /** 状態遷移エラー用（現在状態・許可操作付き）。 */
    public static ApiError transition(String code, String message, String reason,
                                      String currentStatus, List<String> allowedActions) {
        return new ApiError(new ErrorBody(code, message, reason, currentStatus, allowedActions, null));
    }

    /** ポリシー違反など details 付き。 */
    public static ApiError withDetails(String code, String message, List<Object> details) {
        return new ApiError(new ErrorBody(code, message, null, null, null, details));
    }
}
