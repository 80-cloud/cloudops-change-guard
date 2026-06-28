package com.cloudopsguard.domain.execution;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * 実施前チェック1項目（pre_execution_checks）。SCHEDULE 時に雛形から CR ごとに生成し、
 * OPERATOR が完了をマークする。is_required は CR の環境で決まる（本番は必須）。
 */
@Entity
@Table(name = "pre_execution_checks")
@Getter
@Setter
@NoArgsConstructor
public class PreExecutionCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "change_request_id", nullable = false)
    private Long changeRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false, length = 50)
    private CheckType checkType;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
