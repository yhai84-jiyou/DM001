import request from './request'
import type { Category } from '@/types'

export const categoryApi = {
  // Admin
  tree: () => request.get<Category[]>('/api/admin/categories'),
  create: (data: { name: string; parentId?: number; slug: string; icon?: string }) =>
    request.post<Category>('/api/admin/categories', data),
  update: (id: number, data: { name?: string; slug?: string; icon?: string }) =>
    request.put<Category>(`/api/admin/categories/${id}`, data),
  delete: (id: number) => request.delete(`/api/admin/categories/${id}`),
  reorder: (data: Array<{ id: number; parentId: number | null; sortOrder: number }>) =>
    request.post('/api/admin/categories/reorder', data),

  // Public
  publicTree: () => request.get<Category[]>('/api/public/categories'),
}
