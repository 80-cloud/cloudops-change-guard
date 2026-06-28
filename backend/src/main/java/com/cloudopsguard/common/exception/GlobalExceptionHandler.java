package com.cloudopsguard.common.exception;

import com.cloudopsguard.common.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 例外 → 統一エラーレスポンスの変換（API設計.md §0）。
 *
 * <p>方針：未認証 401／認可不可 403／不在・IDOR 404／検証 422／状態遷移・ポリシー・競合 409。
 * スタックトレース・SQL・内部クラス名はレスポンスに出さない。silent catch しない
 * （想定外は 500 にしつつサーバーログには記録する）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 状態遷移エラー（409・現在状態と許可操作付き）。 */
    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ApiError> handleTransition(IllegalStateTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.transition(ex.getCode(), ex.getMessage(), ex.getReason(),
                        ex.getCurrentStatus(), ex.getAllowedActions()));
    }

    /** BLOCK ポリシー違反（409・details 付き）。 */
    @ExceptionHandler(PolicyBlockedException.class)
    public ResponseEntity<ApiError> handlePolicyBlocked(PolicyBlockedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.withDetails(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    /** その他のアプリ業務例外（code/httpStatus は各例外が保持）。 */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiError.of(ex.getCode(), ex.getMessage()));
    }

    /** Bean Validation（@Valid）の失敗（422）。最初のフィールドエラーを返す。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("入力内容を確認してください");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of("VALIDATION_ERROR", msg));
    }

    /** 壊れた/読めない JSON・enum 不一致（400）。401 に化けるのを防ぎここで確定させる。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("MALFORMED_REQUEST", "リクエスト本文の形式が不正です"));
    }

    /** 楽観ロック競合（409・R-03 同時更新の競合検知）。 */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("CONFLICT", "他の操作と競合しました。最新の状態を取得してやり直してください"));
    }

    /** 認可不可（403）：Spring Security のメソッド認可など。 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("FORBIDDEN", "この操作を行う権限がありません"));
    }

    /** 未認証（401）。 */
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiError> handleUnauthenticated(AuthenticationCredentialsNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("UNAUTHENTICATED", "ログインが必要です"));
    }

    /** 想定外（500）。内部情報は返さずサーバーログにのみ記録する（silent catch 禁止）。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("未処理の例外", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "サーバー内部でエラーが発生しました"));
    }
}
