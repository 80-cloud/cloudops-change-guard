package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SSH(22) の全世界公開（risk-rules.json: SSH_22_OPEN_WORLD）。
 * ingress でポート22を 0.0.0.0/0（または ::/0）へ許可する変更。値ベース判定のため diff_text 全体を見る。
 */
@Component
public class Ssh22OpenWorldRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "SSH_22_OPEN_WORLD";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        String t = cr.getDiffText() == null ? "" : cr.getDiffText().toLowerCase();
        boolean openWorld = t.contains("0.0.0.0/0") || t.contains("::/0");
        boolean port22 = t.contains("22")
                && (t.contains("from_port") || t.contains("fromport")
                    || t.contains("ingress") || t.contains("ssh"));
        return openWorld && port22;
    }
}
