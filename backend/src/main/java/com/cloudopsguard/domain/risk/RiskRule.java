package com.cloudopsguard.domain.risk;

import com.cloudopsguard.domain.changerequest.ChangeRequest;

import java.util.List;

/**
 * 検知ルール（リスク判定ルール.md §5：1ルール1クラス・M-3）。
 * 説明文・統制属性は持たず（risk-rules.json＝{@link RiskRuleDefinition} が供給）、検知の述語のみを担う。
 *
 * <p>アクションベースのルールは {@code changes}（正規化済み）を、値ベースのルール
 * （0.0.0.0/0・ポート22 等）は {@code cr.getDiffText()} を参照してよい。
 */
public interface RiskRule {

    /** risk-rules.json と対応するルールコード。 */
    String ruleCode();

    /** この変更が当ルールに該当するか。 */
    boolean detects(List<NormalizedChange> changes, ChangeRequest cr);
}
