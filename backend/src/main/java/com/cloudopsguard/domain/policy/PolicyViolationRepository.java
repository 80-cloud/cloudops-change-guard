package com.cloudopsguard.domain.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyViolationRepository extends JpaRepository<PolicyViolation, Long> {

    List<PolicyViolation> findByChangeRequestId(Long changeRequestId);
}
