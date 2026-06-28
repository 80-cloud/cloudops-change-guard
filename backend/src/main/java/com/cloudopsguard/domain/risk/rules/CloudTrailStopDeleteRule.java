package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.ChangeAction;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CloudTrail の停止または削除（risk-rules.json: CLOUDTRAIL_STOP_DELETE）。
 * 証跡の delete、またはログ記録の無効化。CRITICAL・無条件ブロック・追加承認・理由必須。
 */
@Component
public class CloudTrailStopDeleteRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "CLOUDTRAIL_STOP_DELETE";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        boolean deleted = Detections.anyChange(changes,
                t -> t.contains("cloudtrail"), ChangeAction.DELETE);
        boolean loggingDisabled = Detections.compact(cr).contains("enable_logging=false");
        return deleted || loggingDisabled;
    }
}
