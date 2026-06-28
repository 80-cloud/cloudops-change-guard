package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.ChangeAction;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * S3 バケットの削除（risk-rules.json: S3_BUCKET_DELETE）。
 * 本番では CRITICAL に昇格しブロック（envEscalation/control はデータ駆動）。サブリソース
 * （bucket_policy/versioning 等）は対象外とするため型を厳密一致させる。
 */
@Component
public class S3BucketDeleteRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "S3_BUCKET_DELETE";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        return Detections.anyChange(changes,
                t -> t.equals("aws_s3_bucket") || t.equals("aws::s3::bucket"),
                ChangeAction.DELETE);
    }
}
