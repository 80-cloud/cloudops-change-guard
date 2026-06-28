package com.cloudopsguard.domain.auth;

import com.cloudopsguard.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * ログイン認証失敗（401）。ユーザー名の存在有無を漏らさないため、
 * 「ユーザーがいない」「パスワード不一致」を区別せず同一の汎用エラーにする。
 */
public class BadCredentialsException extends ApiException {

    public BadCredentialsException() {
        super("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED, "ユーザー名またはパスワードが正しくありません");
    }
}
