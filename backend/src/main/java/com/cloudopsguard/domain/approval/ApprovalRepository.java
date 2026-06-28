package com.cloudopsguard.domain.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    List<Approval> findByChangeRequestIdOrderByDecidedAtAsc(Long changeRequestId);
}
