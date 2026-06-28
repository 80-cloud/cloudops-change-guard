package com.cloudopsguard.domain.approval;

import com.cloudopsguard.domain.common.Decision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    List<Approval> findByChangeRequestIdOrderByDecidedAtAsc(Long changeRequestId);

    /** 当該変更の指定 decision の件数（APPROVED の distinct 承認者数＝定足数判定に使う）。 */
    long countByChangeRequestIdAndDecision(Long changeRequestId, Decision decision);
}
