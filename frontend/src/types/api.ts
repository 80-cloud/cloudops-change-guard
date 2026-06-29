export type Environment = 'development' | 'staging' | 'production';
export type IacType = 'TERRAFORM' | 'CLOUDFORMATION';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type Role = 'REQUESTER' | 'REVIEWER' | 'OPERATOR' | 'ADMIN';
export type Decision = 'APPROVED' | 'REJECTED' | 'RETURNED';
export type IacApplyResult = 'SUCCESS' | 'FAILED';
export type CheckType = 'BACKUP' | 'ROLLBACK' | 'MONITORING' | 'IMPACT' | 'STAKEHOLDER' | 'WINDOW' | 'APPROVAL';
export type HealthCheckItem = 'IAC_APPLY' | 'ALB_TARGET_HEALTH' | 'EC2_SSM' | 'HTTP_HEALTH' | 'CW_ALARM' | 'APP_REACHABILITY' | 'DB_CONNECTION' | 'NOTE';
export type HealthResult = 'HEALTHY' | 'WARNING' | 'UNHEALTHY' | 'NOT_CHECKED';
export type ChangeRequestStatus =
  | 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED'
  | 'RETURNED' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'
  | 'ROLLED_BACK' | 'CANCELLED';
export type TransitionAction =
  | 'submit' | 'cancel' | 'review-start' | 'approve' | 'reject'
  | 'return' | 'schedule' | 'start' | 'complete' | 'fail' | 'rollback';
export type AuditActionType =
  | 'CREATE' | 'EDIT' | 'SUBMIT' | 'REVIEW_START' | 'APPROVE' | 'REJECT'
  | 'RETURN' | 'SCHEDULE' | 'EXECUTION_START' | 'EXECUTION_COMPLETE'
  | 'EXECUTION_FAIL' | 'ROLLBACK' | 'CANCEL';

export interface PageMeta { page: number; size: number; totalElements: number; totalPages: number; }
export interface ApiResponse<T> { data: T; meta?: PageMeta; }
export interface PageResponse<T> { content: T[]; meta: PageMeta; }
export interface ApiError { code: string; message: string; currentStatus?: ChangeRequestStatus; allowedActions?: TransitionAction[]; details?: unknown[]; }

export interface MeResponse { id: number; username: string; displayName: string; role: Role; }

export interface ChangeRequestSummary {
  id: number; title: string; targetEnvironment: Environment; iacType: IacType;
  status: ChangeRequestStatus; riskLevel: RiskLevel | null; requesterId: number;
  scheduledAt: string | null; createdAt: string; updatedAt: string;
}
export interface ChangeRequestResponse {
  id: number; title: string; targetEnvironment: Environment; iacType: IacType;
  targetAwsService: string | null; targetResourceName: string | null;
  changeReason: string | null; changeSummary: string | null; diffText: string | null;
  scheduledAt: string | null; rollbackProcedure: string | null;
  status: ChangeRequestStatus; riskLevel: RiskLevel | null; requesterId: number;
  version: number; createdAt: string; updatedAt: string; allowedActions: TransitionAction[];
}
export interface PreCheckResponse { id: number; checkType: CheckType; required: boolean; completed: boolean; completedBy: number | null; completedAt: string | null; }
export interface HealthCheckResponse { id: number; checkItem: HealthCheckItem; result: HealthResult; note: string | null; recordedBy: number; recordedAt: string; }
export interface ExecutionResponse {
  id: number; changeRequestId: number; operatorId: number; iacApplyResult: IacApplyResult;
  serviceHealthConfirmed: boolean; startedAt: string; finishedAt: string | null;
  rollbackPerformed: boolean; rollbackNote: string | null;
}
export interface ChangeRequestDetailResponse {
  changeRequest: ChangeRequestResponse;
  preChecks: PreCheckResponse[];
  healthChecks: HealthCheckResponse[];
  execution: ExecutionResponse | null;
}
export interface RiskFindingResponse {
  ruleCode: string; ruleName: string; riskLevel: RiskLevel;
  whyDangerous: string; expectedImpact: string; recommendedAction: string;
  isBlock: boolean; requiresAdditionalApproval: boolean;
}
export interface RiskAssessmentResponse { riskLevel: RiskLevel; blocked: boolean; requiresAdditionalApproval: boolean; assessedAt: string; findings: RiskFindingResponse[]; }
export interface BlockReason { kind: 'RISK' | 'POLICY'; code: string; message: string; recommendedAction?: string; }
export interface PreviewRiskResponse { riskLevel: RiskLevel; blocked: boolean; requiresAdditionalApproval: boolean; findings: RiskFindingResponse[]; blockReasons: BlockReason[]; }
export interface PolicyViolationResponse { code: string; effect: string; message: string; detectedAt: string; }
export interface ApprovalResponse { id: number; changeRequestId: number; reviewerId: number; decision: Decision; comment: string | null; decidedAt: string; }
export interface AuditLogResponse { id: number; changeRequestId: number; actorId: number; actionType: AuditActionType; beforeStatus: ChangeRequestStatus | null; afterStatus: ChangeRequestStatus | null; comment: string | null; summary: string | null; createdAt: string; }
export interface CommentResponse { id: number; changeRequestId: number; authorId: number; body: string; createdAt: string; }

export interface CreateChangeRequest {
  title: string; targetEnvironment: Environment; iacType: IacType;
  targetAwsService?: string; targetResourceName?: string; changeReason?: string;
  changeSummary?: string; diffText?: string; scheduledAt?: string; rollbackProcedure?: string;
}
export interface TransitionRequest { comment?: string; scheduledAt?: string; }
export interface CreateComment { body: string; }

export interface DashboardSummaryResponse {
  statusCounts: Partial<Record<ChangeRequestStatus, number>>;
  highRisk: ChangeRequestSummary[];
  pendingApproval: ChangeRequestSummary[];
  scheduled: ChangeRequestSummary[];
  recentAudit: AuditLogResponse[];
}

export interface PolicyResponse {
  code: string;
  name: string;
  environmentScope: string;
  effect: string;
  enabled: boolean;
}
