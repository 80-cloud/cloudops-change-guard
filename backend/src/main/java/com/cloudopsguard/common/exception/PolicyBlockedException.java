package com.cloudopsguard.common.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * BLOCK ポリシー違反で遷移できない（409）。違反の詳細（details）を保持する。
 * 評価器の本実装は Phase 3。Phase 2 はフック点（例外型・配線）のみ用意する。
 */
public class PolicyBlockedException extends ApiException {

    private final transient List<Object> details;

    public PolicyBlockedException(String message, List<Object> details) {
        super("POLICY_BLOCKED", HttpStatus.CONFLICT, message);
        this.details = details;
    }

    public List<Object> getDetails() {
        return details;
    }
}
