package com.cloudopsguard.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 入力検証エラー（422）。Bean Validation で表せない業務的な必須充足
 * （例：submit 時の必須項目不足）に使う。
 */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
