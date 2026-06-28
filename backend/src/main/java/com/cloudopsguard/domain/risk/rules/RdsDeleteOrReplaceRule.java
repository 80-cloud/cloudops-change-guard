package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.ChangeAction;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RDS(DBInstance/DBCluster) の削除または置換（risk-rules.json: RDS_DELETE_OR_REPLACE）。
 * 削除保護・最終スナップショット次第で本番データが不可逆に失われるため CRITICAL・本番ブロック。
 */
@Component
public class RdsDeleteOrReplaceRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "RDS_DELETE_OR_REPLACE";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        return changes.stream().anyMatch(c ->
                isRds(c.resourceType())
                        && (c.action() == ChangeAction.DELETE || c.action() == ChangeAction.REPLACE));
    }

    private boolean isRds(String resourceType) {
        if (resourceType == null) {
            return false;
        }
        String t = resourceType.toLowerCase();
        // terraform: aws_db_instance / aws_rds_cluster、CFN: AWS::RDS::DBInstance / DBCluster
        return t.contains("rds") || t.contains("db_instance") || t.contains("db_cluster");
    }
}
