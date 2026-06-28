package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * IAM 管理者権限(AdministratorAccess)の付与（risk-rules.json: IAM_ADMIN_ACCESS）。
 * 全権ポリシーのアタッチ。CRITICAL・本番ブロック・追加承認。値ベース判定。
 */
@Component
public class IamAdminAccessRule implements RiskRule {

    @Override
    public String ruleCode() {
        return "IAM_ADMIN_ACCESS";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        return Detections.lower(cr).contains("administratoraccess");
    }
}
