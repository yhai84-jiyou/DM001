import request from './request'
import type { OperationLog, Page } from '@/types'

export const logApi = {
  list: (params?: { page?: number; size?: number; userId?: number; action?: string; targetType?: string }) =>
    request.get<Page<OperationLog>>('/api/admin/logs', { params }),
}
