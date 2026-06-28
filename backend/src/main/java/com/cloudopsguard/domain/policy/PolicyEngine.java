package com.cloudopsguard.domain.policy;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.risk.RiskAssessmentResult;
import com.cloudopsguard.domain.risk.RiskFinding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ポリシー評価器（ポリシー一覧.md）。リスク判定結果と環境から、統制（BLOCK・追加承認・理由・メンテ枠）を確定する。
 * 責務分離：リスク判定＝危険の説明、ポリシー＝統制の強制（状態遷移設計.md §冒頭）。
 *
 * <p>適用条件：ポリシーが有効かつ環境スコープが合致し、{@code appliesToRuleCodes}（発火した
 * リスクルールコードのいずれか）または {@code appliesToRiskLevels}（集約リスクレベル）に該当すること。
 */
@Component
public class PolicyEngine {

    private final PolicyCatalog catalog;

    public PolicyEngine(PolicyCatalog catalog) {
        this.catalog = catalog;
    }

    public PolicyEvaluationResult evaluate(ChangeRequest cr, RiskAssessmentResult risk) {
        Set<String> firedRuleCodes = risk.findings().stream()
                .map(RiskFinding::ruleCode)
                .collect(Collectors.toSet());

        List<PolicyOutcome> outcomes = new ArrayList<>();
        for (PolicyRuleDefinition def : catalog.all()) {
            if (!def.enabled()) {
                continue;
            }
            if (!scopeMatches(def.environmentScope(), cr.getTargetEnvironment())) {
                continue;
            }
            if (applies(def, firedRuleCodes, risk)) {
                outcomes.add(new PolicyOutcome(def.code(), def.effect(), def.message()));
            }
        }
        return new PolicyEvaluationResult(List.copyOf(outcomes));
    }

    /** ALL か、対象環境（小文字 wire）に一致するか。 */
    private boolean scopeMatches(String scope, Environment env) {
        if (scope == null) {
            return false;
        }
        if (scope.equalsIgnoreCase("ALL")) {
            return true;
        }
        return env != null && scope.equalsIgnoreCase(env.wire());
    }

    private boolean applies(PolicyRuleDefinition def, Set<String> firedRuleCodes,
                            RiskAssessmentResult risk) {
        boolean byRuleCode = def.appliesToRuleCodes() != null
                && def.appliesToRuleCodes().stream().anyMatch(firedRuleCodes::contains);
        boolean byRiskLevel = def.appliesToRiskLevels() != null
                && def.appliesToRiskLevels().contains(risk.riskLevel());
        return byRuleCode || byRiskLevel;
    }
}
