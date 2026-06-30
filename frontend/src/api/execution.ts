import client from './client';
import type {
  ApiResponse, PreCheckResponse, HealthCheckResponse, ExecutionResponse,
  HealthCheckItem, HealthResult, IacApplyResult,
} from '../types/api';

export const completePreCheck = (id: number, checkId: number) =>
  client
    .post<ApiResponse<PreCheckResponse>>(`/change-requests/${id}/pre-checks/${checkId}/complete`)
    .then((r) => r.data.data);

export const recordHealthCheck = (
  id: number,
  body: { checkItem: HealthCheckItem; result: HealthResult; note?: string },
) =>
  client
    .post<ApiResponse<HealthCheckResponse>>(`/change-requests/${id}/health-checks`, body)
    .then((r) => r.data.data);

export const recordExecutionResult = (
  id: number,
  iacApplyResult: IacApplyResult,
  applyRunUrl?: string,
) =>
  client
    .post<ApiResponse<ExecutionResponse>>(`/change-requests/${id}/execution-result`, {
      iacApplyResult,
      applyRunUrl,
    })
    .then((r) => r.data.data);
