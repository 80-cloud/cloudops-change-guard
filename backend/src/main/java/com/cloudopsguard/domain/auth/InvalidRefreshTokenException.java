package com.cloudopsguard.domain.auth;

import com.cloudopsguard.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** refresh トークン無効（401・未知/期限切れ/reuse 検知）。 */
public class InvalidRefreshTokenException extends ApiException {

    public InvalidRefreshTokenException(String message) {
        super("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, message);
    }
}
