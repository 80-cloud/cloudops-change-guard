package com.cloudopsguard.domain.execution;

/** 実施前チェックの種別（pre_execution_checks.check_type）。 */
public enum CheckType {
    BACKUP, ROLLBACK, MONITORING, IMPACT, STAKEHOLDER, WINDOW, APPROVAL
}
