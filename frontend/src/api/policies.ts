import client from './client';
import type { ApiResponse, PolicyResponse } from '../types/api';

export const getPolicies = () =>
  client
    .get<ApiResponse<PolicyResponse[]>>('/policies')
    .then((r) => r.data.data);
