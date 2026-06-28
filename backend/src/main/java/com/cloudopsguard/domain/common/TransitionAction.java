package com.cloudopsguard.domain.common;

/**
 * 状態遷移イベント（API設計.md §2 の遷移エンドポイントと対）。
 * {@link #wire()} はエラー応答の {@code allowedActions} や URL サフィックスに使う表現
 * （例 review-start）。{@code return} は予約語のため列挙名は {@code RETURN_} とする。
 */
public enum TransitionAction {
    SUBMIT("submit"),
    CANCEL("cancel"),
    REVIEW_START("review-start"),
    APPROVE("approve"),
    REJECT("reject"),
    RETURN_("return"),
    SCHEDULE("schedule"),
    START("start"),
    COMPLETE("complete"),
    FAIL("fail"),
    ROLLBACK("rollback");

    private final String wire;

    TransitionAction(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }
}
