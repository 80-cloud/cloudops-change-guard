package com.cloudopsguard.domain.changerequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 変更申請のリポジトリ。一覧の動的フィルタ（environment/status/risk/requesterId）は
 * {@link JpaSpecificationExecutor} ＋ {@link ChangeRequestSpecifications} で組み立てる（全件取得しない＝P-2）。
 */
public interface ChangeRequestRepository
        extends JpaRepository<ChangeRequest, Long>, JpaSpecificationExecutor<ChangeRequest> {
}
