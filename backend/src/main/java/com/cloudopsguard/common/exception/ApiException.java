package com.cloudopsguard.common.exception;

import org.springframework.http.HttpStatus;

/**
 * アプリ共通の業務例外の基底。code（API のエラーコード）と httpStatus を保持し、
 * {@code GlobalExceptionHandler} が統一フォーマットに変換する。
 * 内部情報（stacktrace 等）はレスポンスに出さない。
 */
public abstract class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    protected ApiException(String code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
