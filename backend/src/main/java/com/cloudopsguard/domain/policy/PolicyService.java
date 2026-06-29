package com.cloudopsguard.domain.policy;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** ポリシー定義の参照ユースケース（SCR-06・閲覧のみ）。 */
@Service
public class PolicyService {

    private final PolicyRuleRepository repository;

    public PolicyService(PolicyRuleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PolicyRule> listAll() {
        return repository.findAll(Sort.by("code"));
    }
}
