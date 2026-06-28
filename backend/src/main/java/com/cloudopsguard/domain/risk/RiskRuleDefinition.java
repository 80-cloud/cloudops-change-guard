package com.cloudopsguard.domain.risk;

import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.RiskLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * risk-rules.json の1ルール定義（説明文・統制属性のデータ駆動・リスク判定ルール.md §4.5/§7-1）。
 * 検知ロジックは {@link RiskRule} 実装が担い、本レコードは説明文と統制（control）を供給する。
 *
 * <p>未知プロパティ（followUps / detection / awsZukanRefs / sources など評価器が使わない項目）は無視する。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RiskRuleDefinition(
        String ruleCode,
        String ruleName,
        RiskLevel baseRiskLevel,
        Map<String, RiskLevel> envEscalation,
        String whyDangerous,
        String plainMeaning,
        String expectedImpact,
        String recommendedAction,
        Control control) {

    /** 統制属性。isBlock は無条件ブロック、isBlockInProd は本番のみブロック（どちらか省略可）。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Control(
            Boolean isBlock,
            Boolean isBlockInProd,
            boolean requiresAdditionalApproval,
            boolean requiresReason,
            boolean requiresMaintenanceWindow) {

        /** 指定環境で BLOCK 対象か（無条件ブロック、または本番限定ブロック×本番）。 */
        public boolean isBlockEffective(Environment env) {
            if (Boolean.TRUE.equals(isBlock)) {
                return true;
            }
            return Boolean.TRUE.equals(isBlockInProd) && env == Environment.PRODUCTION;
        }
    }

    /** 環境を加味した実効リスク（envEscalation に該当があれば昇格、無ければ baseRiskLevel）。 */
    public RiskLevel effectiveRisk(Environment env) {
        if (envEscalation != null && env != null) {
            RiskLevel escalated = envEscalation.get(env.wire());
            if (escalated != null) {
                return escalated;
            }
        }
        return baseRiskLevel;
    }
}
