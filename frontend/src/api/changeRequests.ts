import client from './client';
import type {
  ApiResponse, PageResponse, ChangeRequestSummary, ChangeRequestResponse,
  CreateChangeRequest, PreviewRiskResponse, TransitionRequest,
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
