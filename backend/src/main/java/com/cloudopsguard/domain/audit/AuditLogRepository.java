package com.cloudopsguard.domain.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 監査ログのリポジトリ。<b>INSERT / SELECT のみ</b>を公開する。
 * UPDATE / DELETE を行うメソッドは設けない（改ざん不可・受入 A-8）。
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** 当該申請の監査ログ（新しい順）。 */
    List<AuditLog> findByChangeRequestIdOrderByCreatedAtDesc(Long changeRequestId);

    /** 全監査ログ（ADMIN・新しい順・ページング）。 */
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
