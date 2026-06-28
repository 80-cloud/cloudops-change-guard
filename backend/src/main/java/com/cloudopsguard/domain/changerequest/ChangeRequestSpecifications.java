package com.cloudopsguard.domain.changerequest;

import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.RiskLevel;
import org.springframework.data.jpa.domain.Specification;

/**
 * 一覧フィルタの Specification 組み立て。null の条件は無視する（AND 連結）。
 * REQUESTER は自分の申請のみ＝requesterId を<b>サーバーで強制</b>する（P-2・IDOR 対策）。
 */
public final class ChangeRequestSpecifications {

    private ChangeRequestSpecifications() {
    }

    public static Specification<ChangeRequest> filter(Environment environment,
                                                      ChangeRequestStatus status,
                                                      RiskLevel risk,
                                                      Long requesterId) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (environment != null) {
                predicate = cb.and(predicate, cb.equal(root.get("targetEnvironment"), environment));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (risk != null) {
                predicate = cb.and(predicate, cb.equal(root.get("riskLevel"), risk));
            }
            if (requesterId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("requesterId"), requesterId));
            }
            return predicate;
        };
    }
}
