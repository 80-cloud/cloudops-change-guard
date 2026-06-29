import client from './client';
import type { ApiResponse, DashboardSummaryResponse } from '../types/api';

export const getDashboardSummary = () =>
  client
    .get<ApiResponse<DashboardSummaryResponse>>('/dashboard/summary')
    .then((r) => r.data.data);
