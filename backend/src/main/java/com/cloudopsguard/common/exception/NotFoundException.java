package com.cloudopsguard.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 不在・IDOR（所有不一致）時に投げる（404）。
 * 「存在するが権限がない」と「存在しない」を区別せず 404 に揃え、リソースの存在を秘匿する
 * （権限マトリクス.md §3「不一致は 404（存在秘匿）」）。
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super("NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }

    public NotFoundException() {
        this("対象が見つかりません");
    }
}
