package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.ChangeAction;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CloudWatch アラームの削除（risk-rules.json: CW_ALARM_DELETE）。
 * MEDIUM・ブロックなし（理由必須はポリシー側）。監視に穴を作る変更。
 */
@Component
public class CwAlarmDeleteRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "CW_ALARM_DELETE";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        return Detections.anyChange(changes,
                t -> t.equals("aws_cloudwatch_metric_alarm") || t.contains("cloudwatch::alarm"),
                ChangeAction.DELETE);
    }
}
