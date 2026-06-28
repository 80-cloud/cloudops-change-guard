package com.cloudopsguard.domain.execution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExecutionRepository extends JpaRepository<Execution, Long> {

    /** CR の最新の実施記録（CR あたり1行前提だが、安全のため最新を解釈する）。 */
    Optional<Execution> findTopByChangeRequestIdOrderByStartedAtDesc(Long changeRequestId);
}
