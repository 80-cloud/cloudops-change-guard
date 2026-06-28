package com.cloudopsguard.domain.policy;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {

    boolean existsByCode(String code);
}
