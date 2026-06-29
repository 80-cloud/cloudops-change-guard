import axios, { type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';

const client = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
});

let refreshing: Promise<AxiosResponse> | null = null;

client.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const config = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    if (error.response?.status === 401 && config && !config._retried && !config.url?.includes('/auth/')) {
      config._retried = true;
      try {
        refreshing = refreshing ?? client.post('/auth/refresh');
        await refreshing;
        refreshing = null;
        return client(config);
      } catch (e) {
        refreshing = null;
        return Promise.reject(e);
      }
    }
    return Promise.reject(error);
  },
);

export default client;
