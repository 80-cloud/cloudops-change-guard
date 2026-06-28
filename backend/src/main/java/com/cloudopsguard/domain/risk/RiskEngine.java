package com.cloudopsguard.domain.risk;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * リスク判定エンジン（リスク判定ルール.md §1）。
 * diff を IaC 種別ごとに正規化 → 各 {@link RiskRule} で検知 → {@link RiskRuleCatalog} の説明文・統制で
 * finding を組み立て → 集約（risk_level=max・isBlocked=any・追加承認=any）する。
 *
 * <p>純ロジック（DB 非依存）。永続化（risk_assessments/findings）と SUBMIT への配線は後続インクリメント。
 */
@Component
public class RiskEngine {

    private final List<RiskRule> rules;
    private final List<IacDiffParser> parsers;
    private final RiskRuleCatalog catalog;

    public RiskEngine(List<RiskRule> rules, List<IacDiffParser> parsers, RiskRuleCatalog catalog) {
        this.rules = rules;
        this.parsers = parsers;
        this.catalog = catalog;
    }

    /** 変更申請を評価して集約結果を返す（永続化はしない）。 */
    public RiskAssessmentResult assess(ChangeRequest cr) {
        List<NormalizedChange> changes = normalize(cr);

        List<RiskFinding> findings = new ArrayList<>();
        for (RiskRule rule : rules) {
            if (!rule.detects(changes, cr)) {
                continue;
            }
            RiskRuleDefinition def = catalog.byCode(rule.ruleCode());
            RiskLevel level = def.effectiveRisk(cr.getTargetEnvironment());
            boolean block = def.control().isBlockEffective(cr.getTargetEnvironment());
            findings.add(new RiskFinding(
                    def.ruleCode(), def.ruleName(), level,
                    def.whyDangerous(), def.plainMeaning(), def.expectedImpact(), def.recommendedAction(),
                    block, def.control().requiresAdditionalApproval()));
        }

        RiskLevel overall = findings.stream()
                .map(RiskFinding::riskLevel)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(RiskLevel.LOW);
        boolean blocked = findings.stream().anyMatch(RiskFinding::isBlock);
        boolean additionalApproval = findings.stream().anyMatch(RiskFinding::requiresAdditionalApproval);

        return new RiskAssessmentResult(overall, blocked, additionalApproval, List.copyOf(findings));
    }

    /** IaC 種別に対応するパーサで正規化（対応パーサが無ければ空リスト＝何も検知しない）。 */
    private List<NormalizedChange> normalize(ChangeRequest cr) {
        return parsers.stream()
                .filter(p -> p.supports(cr.getIacType()))
                .findFirst()
                .map(p -> p.parse(cr.getDiffText()))
                .orElseGet(List::of);
    }
}
