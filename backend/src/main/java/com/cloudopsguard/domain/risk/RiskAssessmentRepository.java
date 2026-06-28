package com.cloudopsguard.domain.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    /** 変更申請の最新判定（assessed_at 降順の先頭）。 */
    Optional<RiskAssessment> findTopByChangeRequestIdOrderByAssessedAtDesc(Long changeRequestId);
}
