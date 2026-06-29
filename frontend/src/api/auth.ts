import client from './client';
import type { ApiResponse, MeResponse } from '../types/api';

export const login = (username: string, password: string) =>
  client.post<ApiResponse<MeResponse>>('/auth/login', { username, password }).then((r) => r.data.data);

export const logout = () => client.post('/auth/logout');

export const fetchMe = () =>
  client.get<ApiResponse<MeResponse>>('/auth/me').then((r) => r.data.data);
