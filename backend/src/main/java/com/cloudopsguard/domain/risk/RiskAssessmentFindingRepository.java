package com.cloudopsguard.domain.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskAssessmentFindingRepository extends JpaRepository<RiskAssessmentFinding, Long> {

    List<RiskAssessmentFinding> findByRiskAssessmentId(Long riskAssessmentId);
}
