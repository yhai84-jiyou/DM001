import request from './request'
import type { User } from '@/types'

export const userApi = {
  list: () => request.get<User[]>('/api/admin/users'),
  create: (data: { username: string; displayName: string; role: string }) =>
    request.post<{ user: User; password: string }>('/api/admin/users', data),
  update: (id: number, data: { displayName?: string; role?: string }) =>
    request.put<User>(`/api/admin/users/${id}`, data),
  toggleStatus: (id: number) => request.post(`/api/admin/users/${id}/toggle-status`),
  resetPassword: (id: number) => request.post<{ password: string }>(`/api/admin/users/${id}/reset-password`),
}
