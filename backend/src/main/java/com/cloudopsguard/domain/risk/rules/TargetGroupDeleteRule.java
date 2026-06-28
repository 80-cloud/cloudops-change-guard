package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.ChangeAction;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ターゲットグループの削除（risk-rules.json: TARGET_GROUP_DELETE）。
 * HIGH・追加承認。ALB からアプリへの転送先が失われ、ルーティングが喪失する。
 */
@Component
public class TargetGroupDeleteRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "TARGET_GROUP_DELETE";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        return Detections.anyChange(changes,
                t -> t.equals("aws_lb_target_group") || t.equals("aws_alb_target_group")
                        || t.contains("elasticloadbalancingv2::targetgroup"),
                ChangeAction.DELETE);
    }
}
