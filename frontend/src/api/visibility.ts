import request from './request'

export const visibilityApi = {
  tree: () => request.get('/api/admin/visibility/tree'),
  toggleCategory: (id: number, data: { visible: boolean }) =>
    request.put(`/api/admin/visibility/category/${id}`, data),
  toggleArticle: (id: number, data: { visible: boolean }) =>
    request.put(`/api/admin/visibility/article/${id}`, data),
  batch: (data: { type: string; ids: number[]; visible: boolean }) =>
    request.post('/api/admin/visibility/batch', data),
}
