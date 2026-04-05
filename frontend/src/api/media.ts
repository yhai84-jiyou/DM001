import request from './request'
import type { Media, Page } from '@/types'

export const mediaApi = {
  upload: (formData: FormData, onProgress?: (percent: number) => void) =>
    request.post<Media>('/api/admin/media/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600000, // 10 min for large files
      onUploadProgress: (e) => {
        if (onProgress && e.total) {
          onProgress(Math.round((e.loaded * 100) / e.total))
        }
      },
    }),
  list: (params?: { page?: number; size?: number }) =>
    request.get<Page<Media>>('/api/admin/media', { params }),
  delete: (id: number) => request.delete(`/api/admin/media/${id}`),
}
