package com.cloudopsguard.domain.risk;

import com.cloudopsguard.domain.common.IacType;

import java.util.List;

/**
 * IaC 種別ごとの差分パーサ（リスク判定ルール.md §1：抽出器は IaC 種別ごとに分離し交換可能にする）。
 * 差分テキストを {@link NormalizedChange} の列へ正規化する。
 */
public interface IacDiffParser {

    /** この実装が扱える IaC 種別か。 */
    boolean supports(IacType iacType);

    /** 差分テキストを正規化済み変更の列にする（解析できなければ空リスト）。 */
    List<NormalizedChange> parse(String diffText);
}
