package com.cloudopsguard.domain.policy;

import com.cloudopsguard.domain.common.RiskLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * policy-rules.json の1要素（評価器の契約）。DB の {@link PolicyRule} には載らない
 * appliesToRuleCodes / appliesToRiskLevels / message を含み、{@link PolicyEngine} が参照する。
 *
 * <p>適用条件は「該当ルールコードを含む」または「該当リスクレベル」のいずれか
 * （両方 null のルールは適用対象外）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyRuleDefinition(
        String code,
        String name,
        String description,
        String environmentScope,
        PolicyEffect effect,
        List<String> appliesToRuleCodes,
        List<RiskLevel> appliesToRiskLevels,
        String message,
        boolean enabled) {
}
