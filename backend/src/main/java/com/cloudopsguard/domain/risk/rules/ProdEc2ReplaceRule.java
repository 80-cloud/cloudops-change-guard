package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.risk.ChangeAction;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本番EC2インスタンスの置換（risk-rules.json: PROD_EC2_REPLACE）。
 * environment=production かつ EC2 が replace(再作成)される変更のみ該当。HIGH・追加承認・メンテ枠必須。
 */
@Component
public class ProdEc2ReplaceRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "PROD_EC2_REPLACE";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        if (cr.getTargetEnvironment() != Environment.PRODUCTION) {
            return false;
        }
        return Detections.anyChange(changes,
                t -> t.equals("aws_instance") || t.contains("ec2::instance"),
                ChangeAction.REPLACE);
    }
}
