package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.ChangeAction;
import com.cloudopsguard.domain.risk.NormalizedChange;
import com.cloudopsguard.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Terraform destroy 相当の一括削除（risk-rules.json: TF_DESTROY_BULK）。
 * plan に複数リソースの destroy/replace が含まれる、または {@code terraform destroy} 相当。
 * 意図しない巻き添え削除が混ざりやすく CRITICAL・本番ブロック。
 */
@Component
public class TfDestroyBulkRule implements RiskRule {

    /** 一括とみなす destroy/replace の件数しきい値。 */
    private static final long BULK_THRESHOLD = 2;

    @Override
    public String ruleCode() {
        return "TF_DESTROY_BULK";
    }

    @Override
    public boolean detects(List<NormalizedChange> changes, ChangeRequest cr) {
        long destroys = changes.stream()
                .filter(c -> c.action() == ChangeAction.DELETE || c.action() == ChangeAction.REPLACE)
                .count();
        if (destroys >= BULK_THRESHOLD) {
            return true;
        }
        String t = cr.getDiffText() == null ? "" : cr.getDiffText().toLowerCase();
        return t.contains("terraform destroy");
    }
}
