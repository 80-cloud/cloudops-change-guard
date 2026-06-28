package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.ChangeAction;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AWS Config の停止または削除（risk-rules.json: AWSCONFIG_STOP_DELETE）。
 * recorder/rule の delete、または記録停止。HIGH・ブロックなし・追加承認・理由必須。
 */
@Component
public class AwsConfigStopDeleteRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "AWSCONFIG_STOP_DELETE";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        boolean deleted = Detections.anyChange(changes,
                t -> t.contains("config_configuration_recorder") || t.contains("config_config_rule")
                        || t.contains("config::configurationrecorder") || t.contains("config::configrule"),
                ChangeAction.DELETE);
        boolean recordingDisabled = Detections.compact(cr).contains("is_enabled=false");
        return deleted || recordingDisabled;
    }
}
