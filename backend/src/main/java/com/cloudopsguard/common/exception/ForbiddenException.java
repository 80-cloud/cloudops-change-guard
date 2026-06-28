package com.cloudopsguard.common.exception;

import org.springframework.http.HttpStatus;

/** 認可なし（403）。ロール不足・操作不可など。 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", HttpStatus.FORBIDDEN, message);
    }

    public ForbiddenException() {
        this("この操作を行う権限がありません");
    }
}
