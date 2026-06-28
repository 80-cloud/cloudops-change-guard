package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.ChangeAction;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ALB(ロードバランサ)の削除（risk-rules.json: ALB_DELETE）。
 * HIGH・追加承認。ターゲットグループ（{@code aws_lb_target_group}）とは別ルールのため、
 * ロードバランサ本体の型のみに厳密一致させる。
 */
@Component
public class AlbDeleteRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "ALB_DELETE";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        return Detections.anyChange(changes,
                t -> t.equals("aws_lb") || t.equals("aws_alb")
                        || t.contains("elasticloadbalancingv2::loadbalancer"),
                ChangeAction.DELETE);
    }
}
