import request from './request'
import type { LoginRequest, LoginResponse, User } from '@/types'

export const authApi = {
  login: (data: LoginRequest) => request.post<LoginResponse>('/api/admin/auth/login', data),
  setup: (data: { username: string; password: string; displayName: string }) => request.post<LoginResponse>('/api/admin/auth/setup', data),
  me: () => request.get<User>('/api/admin/auth/me'),
  logout: () => request.post('/api/admin/auth/logout'),
  needSetup: () => request.get<{ needSetup: boolean }>('/api/admin/auth/setup-status'),
}
