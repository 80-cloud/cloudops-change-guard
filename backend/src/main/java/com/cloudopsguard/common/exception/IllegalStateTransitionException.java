package com.cloudopsguard.common.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * 不正な状態遷移・ガード未充足（409 Conflict）。状態遷移設計.md §5 の形式で、
 * 現在状態（currentStatus）とその利用者が実行可能な操作（allowedActions）を保持する。
 */
public class IllegalStateTransitionException extends ApiException {

    private final String reason;
    private final String currentStatus;
    private final List<String> allowedActions;

    public IllegalStateTransitionException(String message, String reason,
                                           String currentStatus, List<String> allowedActions) {
        super("ILLEGAL_STATE_TRANSITION", HttpStatus.CONFLICT, message);
        this.reason = reason;
        this.currentStatus = currentStatus;
        this.allowedActions = allowedActions;
    }

    public String getReason() {
        return reason;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public List<String> getAllowedActions() {
        return allowedActions;
    }
}
