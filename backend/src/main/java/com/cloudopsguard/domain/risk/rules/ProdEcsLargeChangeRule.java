package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本番ECSサービスの大規模変更（risk-rules.json: PROD_ECS_LARGE_CHANGE）。
 * environment=production かつ ECS service の desiredCount/taskDefinition が変わる変更。HIGH・追加承認。
 * 「大規模」の厳密判定は正規化器の強化に委ね、MVP では対象プロパティの出現で検知する。
 */
@Component
public class ProdEcsLargeChangeRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "PROD_ECS_LARGE_CHANGE";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        if (cr.getTargetEnvironment() != Environment.PRODUCTION) {
            return false;
        }
        String t = Detections.lower(cr);
        boolean ecsService = t.contains("aws_ecs_service") || t.contains("ecs::service");
        boolean scaleOrDef = t.contains("desired_count") || t.contains("desiredcount")
                || t.contains("task_definition") || t.contains("taskdefinition");
        return ecsService && scaleOrDef;
    }
}
