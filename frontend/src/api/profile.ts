import request from './request'
import type { User } from '@/types'

export const profileApi = {
  get: () => request.get<User>('/api/admin/profile'),
  update: (data: { displayName: string }) => request.put('/api/admin/profile', data),
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    request.post('/api/admin/profile/change-password', data),
}
