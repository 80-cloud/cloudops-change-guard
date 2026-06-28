package com.cloudopsguard.domain.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {

    boolean existsByCode(String code);

    Optional<PolicyRule> findByCode(String code);
}
