import request from './request'
import type { Article, ArticleVersion, Page, PublicArticle } from '@/types'

export const articleApi = {
  // Admin
  list: (params?: { page?: number; size?: number; status?: string; categoryId?: number }) =>
    request.get<Page<Article>>('/api/admin/articles', { params }),
  get: (id: number) => request.get<Article>(`/api/admin/articles/${id}`),
  create: (data: { categoryId: number; title: string; editorMode: string; draftContent?: string }) =>
    request.post<Article>('/api/admin/articles', data),
  update: (id: number, data: { title?: string; draftContent?: string; draftContentHtml?: string; summary?: string; categoryId?: number }) =>
    request.put<Article>(`/api/admin/articles/${id}`, data),
  delete: (id: number) => request.post(`/api/admin/articles/${id}/trash`),
  permanentDelete: (id: number) => request.delete(`/api/admin/articles/${id}`),
  restore: (id: number) => request.post(`/api/admin/articles/${id}/restore`),
  trash: (params?: { page?: number; size?: number }) =>
    request.get<Page<Article>>('/api/admin/articles/trash', { params }),
  publish: (id: number, data: { versionLabel: string; changeNotes: string }) =>
    request.post(`/api/admin/articles/${id}/publish`, data),
  unpublish: (id: number) => request.post(`/api/admin/articles/${id}/unpublish`),
  versions: (id: number) => request.get<ArticleVersion[]>(`/api/admin/articles/${id}/versions`),
  revert: (id: number, versionId: number) =>
    request.post(`/api/admin/articles/${id}/revert/${versionId}`),
  importFile: (formData: FormData) =>
    request.post<Article>('/api/admin/articles/import', formData),

  // Public
  getPublic: (slug: string) => request.get<PublicArticle>(`/api/public/articles/${slug}`),
  listByCategory: (categoryId: number) =>
    request.get<PublicArticle[]>(`/api/public/categories/${categoryId}/articles`),
  search: (params: { q: string; page?: number; size?: number }) =>
    request.get<Page<PublicArticle>>('/api/public/search', { params }),
}
