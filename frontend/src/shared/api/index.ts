import api, { clearAuthSession, createApiError, refreshAccessToken } from './client';
import { getApiBaseUrl } from './baseUrl';
import { connectSse } from './sseFetch';
import type {
  AuthResponse,
  AiAssistResponse,
  DashboardStats,
  Equipment,
  EquipmentHistory,
  Failure,
  Intervention,
  PageResponse,
  RecurringDefectItem,
  RecurringDefectsAnalysis,
  User,
} from '@/shared/types';

export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthResponse>('/api/v1/auth/login', { email, password }).then((r) => r.data),
  logout: () => api.post('/api/v1/auth/logout'),
};

export const usersApi = {
  list: () => api.get<User[]>('/api/v1/users').then((r) => r.data),
  assignable: () => api.get<User[]>('/api/v1/users/assignable').then((r) => r.data),
  create: (data: Partial<User> & { password: string }) => api.post<User>('/api/v1/users', data).then((r) => r.data),
  update: (id: string, data: Partial<User> & { password?: string }) => api.put<User>(`/api/v1/users/${id}`, data).then((r) => r.data),
  delete: (id: string) => api.delete(`/api/v1/users/${id}`),
};

export const equipmentApi = {
  list: (params?: Record<string, string | number>) =>
    api.get<PageResponse<Equipment>>('/api/v1/equipment', { params }).then((r) => r.data),
  get: (id: string) => api.get<Equipment>(`/api/v1/equipment/${id}`).then((r) => r.data),
  history: (id: string) => api.get<EquipmentHistory>(`/api/v1/equipment/${id}/history`).then((r) => r.data),
  create: (data: Partial<Equipment>) => api.post<Equipment>('/api/v1/equipment', data).then((r) => r.data),
  update: (id: string, data: Partial<Equipment>) => api.put<Equipment>(`/api/v1/equipment/${id}`, data).then((r) => r.data),
  delete: (id: string) => api.delete(`/api/v1/equipment/${id}`),
};

export const failuresApi = {
  list: (params?: Record<string, string | number>) =>
    api.get<PageResponse<Failure>>('/api/v1/failures', { params }).then((r) => r.data),
  get: (id: string) => api.get<Failure>(`/api/v1/failures/${id}`).then((r) => r.data),
  create: (data: Partial<Failure>) => api.post<Failure>('/api/v1/failures', data).then((r) => r.data),
  update: (id: string, data: Partial<Failure>) => api.put<Failure>(`/api/v1/failures/${id}`, data).then((r) => r.data),
  delete: (id: string) => api.delete(`/api/v1/failures/${id}`),
};

export const interventionsApi = {
  list: (failureId: string) =>
    api.get<PageResponse<Intervention>>('/api/v1/interventions', { params: { failureId } }).then((r) => r.data),
  get: (id: string) => api.get<Intervention>(`/api/v1/interventions/${id}`).then((r) => r.data),
  create: (data: Partial<Intervention>) => api.post<Intervention>('/api/v1/interventions', data).then((r) => r.data),
  update: (id: string, data: Partial<Intervention>) => api.put<Intervention>(`/api/v1/interventions/${id}`, data).then((r) => r.data),
  submit: (id: string) => api.post<Intervention>(`/api/v1/interventions/${id}/submit`).then((r) => r.data),
  validate: (id: string, approved: boolean, commentaire?: string) =>
    api.post<Intervention>(`/api/v1/interventions/${id}/validate`, { approved, commentaire }).then((r) => r.data),
  uploadDocument: async (id: string, file: File) => {
    const url = `${getApiBaseUrl()}/api/v1/interventions/${id}/documents`;

    const send = async (accessToken: string | null) => {
      const form = new FormData();
      form.append('file', file);
      const headers: Record<string, string> = {};
      if (accessToken) {
        headers.Authorization = `Bearer ${accessToken}`;
      }
      return fetch(url, { method: 'POST', headers, body: form });
    };

    let response = await send(localStorage.getItem('accessToken'));

    if (response.status === 401 || response.status === 403) {
      const newToken = await refreshAccessToken();
      if (!newToken) {
        clearAuthSession();
        window.location.href = '/login';
        throw createApiError(response.status, { message: 'Session expirée.' }, response.statusText);
      }
      response = await send(newToken);
    }

    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw createApiError(response.status, data, response.statusText);
    }
    return data;
  },
  downloadDocument: async (interventionId: string, documentId: string, filename: string) => {
    const response = await api.get(
      `/api/v1/interventions/${interventionId}/documents/${documentId}/download`,
      { responseType: 'blob' },
    );
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
  deleteDocument: (interventionId: string, documentId: string) =>
    api.delete(`/api/v1/interventions/${interventionId}/documents/${documentId}`),
  exportPdf: async (interventionId: string) => {
    const response = await api.get(`/api/v1/interventions/${interventionId}/export/pdf`, {
      responseType: 'blob',
    });
    const contentDisposition = response.headers['content-disposition'] as string | undefined;
    const filename = contentDisposition?.includes('filename="')
      ? contentDisposition.split('filename="')[1]?.slice(0, -1)
      : `intervention-${interventionId}.pdf`;
    const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename || `intervention-${interventionId}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};

export interface SearchResponse {
  interventions: Intervention[];
  totalElements: number;
  totalPages: number;
  page: number;
}

export const searchApi = {
  search: (params: Record<string, string | number>) =>
    api.get<SearchResponse>('/api/v1/search', { params }).then((r) => r.data),
};

export const dashboardApi = {
  stats: () => api.get<DashboardStats>('/api/v1/dashboard').then((r) => r.data),
};

export const analyticsApi = {
  recurringDefects: (limit = 10) =>
    api.get<{ defects: RecurringDefectItem[]; totalRecurringCodes: number }>(
      '/api/v1/analytics/recurring-defects',
      { params: { limit } },
    ).then((r) => r.data),
  analyzeRecurringDefects: (limit = 10) =>
    api.post<RecurringDefectsAnalysis>(
      '/api/v1/analytics/recurring-defects/analyze',
      null,
      { params: { limit }, timeout: 120_000 },
    ).then((r) => r.data),
};

export interface AiAssistOptions {
  failureId?: string;
  equipmentId?: string;
  topK?: number;
  signal?: AbortSignal;
  // Streaming callbacks
  onStatus?: (status: string) => void;
  onContext?: (context: string) => void;
  onToken?: (token: string, fullContent: string) => void;
}

export interface StreamEvent {
  event: 'status' | 'context' | 'token' | 'complete' | 'error';
  data: string;
}

export function buildAssistStreamUrl(description: string, options: AiAssistOptions = {}): string {
  const { failureId, equipmentId, topK = 3 } = options;
  const params = new URLSearchParams();
  params.append('description', description);
  if (failureId) params.append('failureId', failureId);
  if (equipmentId) params.append('equipmentId', equipmentId);
  params.append('topK', topK.toString());
  return `${getApiBaseUrl()}/api/v1/ai/assist/stream?${params.toString()}`;
}

export const aiApi = {
  assist: (description: string, options: AiAssistOptions = {}) => {
    const { failureId, equipmentId, topK = 5, signal } = options;
    return api
      .post<AiAssistResponse>(
        '/api/v1/ai/assist',
        { description, failureId, equipmentId, topK },
        { timeout: 120_000, signal },
      )
      .then((r) => r.data);
  },
  
  assistStream: (description: string, options: AiAssistOptions = {}) => {
    return new Promise<AiAssistResponse>((resolve, reject) => {
      const tokens: string[] = [];
      let completed = false;
      let sawErrorEvent = false;
      const controller = new AbortController();

      const finish = (fn: () => void) => {
        if (completed) return;
        completed = true;
        controller.abort();
        fn();
      };

      if (options.signal) {
        options.signal.addEventListener('abort', () => {
          finish(() => reject(new Error('Request was aborted')));
        });
      }

      void connectSse(
        buildAssistStreamUrl(description, options),
        {
          onEvent: (event, data) => {
            if (event === 'status' || event === 'fallback') {
              options.onStatus?.(data);
              return;
            }
            if (event === 'context') {
              options.onContext?.(data);
              return;
            }
            if (event === 'token') {
              tokens.push(data);
              options.onToken?.(data, tokens.join(''));
              return;
            }
            if (event === 'error') {
              sawErrorEvent = true;
              if (data) options.onStatus?.(data);
              return;
            }
            if (event === 'complete') {
              try {
                const parsed = JSON.parse(data) as AiAssistResponse | AiAssistResponse['suggestions'];
                const response: AiAssistResponse =
                  'suggestions' in parsed && parsed.suggestions
                    ? parsed
                    : {
                        similarInterventions: [],
                        suggestions: parsed as AiAssistResponse['suggestions'],
                        disclaimer:
                          "Assistance uniquement — les décisions finales restent celles du technicien ou de l'ingénieur.",
                        diagnosticTrace: null,
                      };
                finish(() => resolve(response));
              } catch {
                finish(() => reject(new Error('Failed to parse complete response')));
              }
            }
          },
          onError: () => {
            if (completed) return;
            finish(() =>
              reject(
                new Error(
                  sawErrorEvent || tokens.length > 0
                    ? 'Stream ended before complete response'
                    : 'SSE connection failed',
                ),
              ),
            );
          },
        },
        { signal: controller.signal },
      );
    });
  },
};

export const liveApi = {
  status: () => api.get<import('@/features/live/types').LiveStatus>('/api/v1/live/status').then((r) => r.data),
};
