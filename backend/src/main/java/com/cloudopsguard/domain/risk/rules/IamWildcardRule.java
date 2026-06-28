package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * IAM の過剰権限 Action:* / Resource:*（risk-rules.json: IAM_WILDCARD）。
 * HIGH・ブロックなし・追加承認。JSON ポリシー記法と HCL 記法の両方を空白除去後に照合する。
 */
@Component
public class IamWildcardRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "IAM_WILDCARD";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        String c = Detections.compact(cr);
        return c.contains("\"action\":\"*\"")
                || c.contains("\"resource\":\"*\"")
                || c.contains("actions=[\"*\"]")
                || c.contains("resources=[\"*\"]")
                || c.contains("action=\"*\"")
                || c.contains("resource=\"*\"");
    }
}
