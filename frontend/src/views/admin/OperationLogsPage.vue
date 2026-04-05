<template>
  <div class="operation-logs-page">
    <div class="page-header">
      <h2>操作日志</h2>
    </div>

    <el-card>
      <div class="filter-bar">
        <el-select v-model="filters.userId" placeholder="操作人" clearable style="width: 160px" @change="loadLogs">
          <el-option
            v-for="u in userOptions"
            :key="u.id"
            :label="u.displayName"
            :value="u.id"
          />
        </el-select>
        <el-select v-model="filters.action" placeholder="操作类型" clearable style="width: 160px" @change="loadLogs">
          <el-option v-for="a in actionOptions" :key="a.value" :label="a.label" :value="a.value" />
        </el-select>
        <el-button @click="loadLogs">查询</el-button>
      </div>

      <el-table :data="logs" stripe v-loading="loading" style="width: 100%">
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="displayName" label="操作人" width="120" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            {{ translateAction(row.action) }}
          </template>
        </el-table-column>
        <el-table-column label="目标" width="100">
          <template #default="{ row }">
            {{ translateTarget(row.targetType) }}
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { logApi } from '@/api/log'
import { userApi } from '@/api/user'
import type { OperationLog, User } from '@/types'

const logs = ref<OperationLog[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const userOptions = ref<User[]>([])

const filters = ref({
  userId: undefined as number | undefined,
  action: '' as string,
})

const actionOptions = [
  { label: '创建文章', value: 'CREATE_ARTICLE' },
  { label: '编辑文章', value: 'UPDATE_ARTICLE' },
  { label: '发布文章', value: 'PUBLISH' },
  { label: '取消发布', value: 'UNPUBLISH' },
  { label: '删除文章', value: 'DELETE_ARTICLE' },
  { label: '恢复文章', value: 'RESTORE_ARTICLE' },
  { label: '创建分类', value: 'CREATE_CATEGORY' },
  { label: '编辑分类', value: 'UPDATE_CATEGORY' },
  { label: '删除分类', value: 'DELETE_CATEGORY' },
  { label: '上传媒体', value: 'UPLOAD_MEDIA' },
  { label: '删除媒体', value: 'DELETE_MEDIA' },
  { label: '创建用户', value: 'CREATE_USER' },
  { label: '编辑用户', value: 'UPDATE_USER' },
  { label: '切换可见性', value: 'TOGGLE_VISIBILITY' },
  { label: '回滚文章', value: 'REVERT_ARTICLE' },
]

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

function translateAction(action: string): string {
  const found = actionOptions.find(a => a.value === action)
  return found ? found.label : action
}

function translateTarget(targetType: string): string {
  const map: Record<string, string> = {
    ARTICLE: '文章',
    CATEGORY: '分类',
    MEDIA: '媒体',
    USER: '用户',
  }
  return map[targetType] || targetType
}

async function loadLogs() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: currentPage.value - 1,
      size: pageSize.value,
    }
    if (filters.value.userId) params.userId = filters.value.userId
    if (filters.value.action) params.action = filters.value.action

    const { data } = await logApi.list(params as Parameters<typeof logApi.list>[0])
    logs.value = data.content
    total.value = data.totalElements
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const { data } = await userApi.list()
    userOptions.value = data
  } catch {
    // ignore - users dropdown might fail for non-admin
  }
  loadLogs()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
