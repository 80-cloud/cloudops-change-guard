import client from './client';
import type {
  ApiResponse, PageResponse, ChangeRequestSummary,
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
