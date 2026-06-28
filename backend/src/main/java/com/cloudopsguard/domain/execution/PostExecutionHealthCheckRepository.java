package com.cloudopsguard.domain.execution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostExecutionHealthCheckRepository extends JpaRepository<PostExecutionHealthCheck, Long> {

    List<PostExecutionHealthCheck> findByChangeRequestIdOrderByRecordedAtAsc(Long changeRequestId);
}
