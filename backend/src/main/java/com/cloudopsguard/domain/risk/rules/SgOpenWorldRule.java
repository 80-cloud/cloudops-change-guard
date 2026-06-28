package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * セキュリティグループの 0.0.0.0/0 公開（risk-rules.json: SG_OPEN_WORLD）。
 * ingress を全世界へ許可する変更。HIGH・無条件ブロック。値ベースのため diff_text を見る。
 */
@Component
public class SgOpenWorldRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "SG_OPEN_WORLD";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        String t = Detections.lower(cr);
        boolean openWorld = t.contains("0.0.0.0/0") || t.contains("::/0");
        boolean ingressContext = t.contains("ingress") || t.contains("cidr_blocks")
                || t.contains("cidrip") || t.contains("security_group") || t.contains("securitygroup");
        return openWorld && ingressContext;
    }
}
