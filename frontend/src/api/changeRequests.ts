import { isAxiosError } from 'axios';
import client from './client';
import type {
  ApiResponse, PageResponse, ChangeRequestSummary, ChangeRequestResponse,
  ChangeRequestDetailResponse, CreateChangeRequest, PreviewRiskResponse, TransitionRequest,
  TransitionAction, ApprovalResponse, CommentResponse, AuditLogResponse,
  RiskAssessmentResponse, PolicyViolationResponse,
  Environment, ChangeRequestStatus, RiskLevel,
} from '../types/api';

export interface ListParams {
  environment?: Environment;
  status?: ChangeRequestStatus;
  risk?: RiskLevel;
  requesterId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export const listChangeRequests = (params: ListParams = {}) =>
  client
    .get<ApiResponse<PageResponse<ChangeRequestSummary>>>('/change-requests', { params })
    .then((r) => r.data.data);

export const getChangeRequest = (id: number) =>
  client
    .get<ApiResponse<ChangeRequestDetailResponse>>(`/change-requests/${id}`)
    .then((r) => r.data.data);

export const createChangeRequest = (body: CreateChangeRequest) =>
  client
    .post<ApiResponse<ChangeRequestResponse>>('/change-requests', body)
    .then((r) => r.data.data);

export const previewRisk = (body: CreateChangeRequest) =>
  client
    .post<ApiResponse<PreviewRiskResponse>>('/change-requests/preview-risk', body)
    .then((r) => r.data.data);

export const submitChangeRequest = (id: number, body: TransitionRequest = {}) =>
  client
    .post<ApiResponse<ChangeRequestResponse>>(`/change-requests/${id}/submit`, body)
    .then((r) => r.data.data);

export const transitionChangeRequest = (id: number, action: TransitionAction, body: TransitionRequest = {}) =>
  client
    .post<ApiResponse<ChangeRequestResponse>>(`/change-requests/${id}/${action}`, body)
    .then((r) => r.data.data);

export const createComment = (id: number, body: string) =>
  client
    .post<ApiResponse<CommentResponse>>(`/change-requests/${id}/comments`, { body })
    .then((r) => r.data.data);

export const getApprovals = (id: number) =>
  client
    .get<ApiResponse<ApprovalResponse[]>>(`/change-requests/${id}/approvals`)
    .then((r) => r.data.data);

export const getComments = (id: number) =>
  client
    .get<ApiResponse<CommentResponse[]>>(`/change-requests/${id}/comments`)
    .then((r) => r.data.data);

export const getAuditLogs = (id: number) =>
  client
    .get<ApiResponse<AuditLogResponse[]>>(`/change-requests/${id}/audit-logs`)
    .then((r) => r.data.data);

export const getPolicyViolations = (id: number) =>
  client
    .get<ApiResponse<PolicyViolationResponse[]>>(`/change-requests/${id}/policy-violations`)
    .then((r) => r.data.data);

export const getRiskAssessment = (id: number): Promise<RiskAssessmentResponse | null> =>
  client
    .get<ApiResponse<RiskAssessmentResponse>>(`/change-requests/${id}/risk-assessment`)
    .then((r) => r.data.data)
    .catch((e) => {
      if (isAxiosError(e) && e.response?.status === 404) return null;
      throw e;
    });
