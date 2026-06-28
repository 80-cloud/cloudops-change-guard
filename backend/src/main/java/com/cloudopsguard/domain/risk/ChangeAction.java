package com.cloudopsguard.domain.risk;

/**
 * 正規化された変更のアクション（リスク判定ルール.md §1）。
 * IaC の差分テキストから抽出する。判定不能は {@link #UNKNOWN}。
 */
public enum ChangeAction {
    CREATE,
    UPDATE,
    DELETE,
    REPLACE,
    UNKNOWN
}
