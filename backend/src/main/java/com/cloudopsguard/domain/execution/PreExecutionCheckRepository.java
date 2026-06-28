package com.cloudopsguard.domain.execution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreExecutionCheckRepository extends JpaRepository<PreExecutionCheck, Long> {

    boolean existsByChangeRequestId(Long changeRequestId);

    List<PreExecutionCheck> findByChangeRequestIdOrderByIdAsc(Long changeRequestId);

    Optional<PreExecutionCheck> findByIdAndChangeRequestId(Long id, Long changeRequestId);
}
