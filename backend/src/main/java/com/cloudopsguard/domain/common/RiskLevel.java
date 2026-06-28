package com.cloudopsguard.domain.common;

/**
 * リスクレベル。宣言順（ordinal）が深刻度の昇順になっており、承認段数判定などで大小比較に使う
 * （LOW &lt; MEDIUM &lt; HIGH &lt; CRITICAL）。Phase 3 の RiskEngine が判定する。
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /** this が other 以上の深刻度か。 */
    public boolean atLeast(RiskLevel other) {
        return this.ordinal() >= other.ordinal();
    }
}
