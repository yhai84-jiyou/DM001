<template>
  <div class="dashboard-page">
    <h2 class="page-title">欢迎回来，{{ authStore.user?.displayName }}</h2>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number">{{ stats.totalArticles }}</div>
          <div class="stat-label">文章总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number published">{{ stats.publishedCount }}</div>
          <div class="stat-label">已发布</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number draft">{{ stats.draftCount }}</div>
          <div class="stat-label">草稿</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number">{{ stats.totalCategories }}</div>
          <div class="stat-label">分类总数</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>最近操作</span>
          </template>
          <el-table :data="recentLogs" stripe style="width: 100%" v-loading="logsLoading">
            <el-table-column prop="createdAt" label="时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="displayName" label="操作人" width="120" />
            <el-table-column prop="action" label="操作" width="140">
              <template #default="{ row }">
                {{ translateAction(row.action) }}
              </template>
            </el-table-column>
            <el-table-column prop="detail" label="详情" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/admin/articles/new')">
              <el-icon><Plus /></el-icon>
              新建文章
            </el-button>
            <el-button @click="$router.push('/admin/categories')">
              <el-icon><Folder /></el-icon>
              管理分类
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Folder } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { articleApi } from '@/api/article'
import { categoryApi } from '@/api/category'
import { logApi } from '@/api/log'
import type { OperationLog } from '@/types'

const authStore = useAuthStore()

const stats = ref({
  totalArticles: 0,
  publishedCount: 0,
  draftCount: 0,
  totalCategories: 0,
})

const recentLogs = ref<OperationLog[]>([])
const logsLoading = ref(false)

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN')
}

function translateAction(action: string): string {
  const map: Record<string, string> = {
    CREATE_ARTICLE: '创建文章',
    UPDATE_ARTICLE: '编辑文章',
    PUBLISH: '发布文章',
    UNPUBLISH: '取消发布',
    DELETE_ARTICLE: '删除文章',
    RESTORE_ARTICLE: '恢复文章',
    CREATE_CATEGORY: '创建分类',
    UPDATE_CATEGORY: '编辑分类',
    DELETE_CATEGORY: '删除分类',
    REORDER_CATEGORY: '分类排序',
    UPLOAD_MEDIA: '上传媒体',
    DELETE_MEDIA: '删除媒体',
    CREATE_USER: '创建用户',
    UPDATE_USER: '编辑用户',
    TOGGLE_USER_STATUS: '切换用户状态',
    RESET_PASSWORD: '重置密码',
    TOGGLE_VISIBILITY: '切换可见性',
    REVERT_ARTICLE: '回滚文章',
  }
  return map[action] || action
}

function countCategories(tree: Array<{ children?: unknown[] }>): number {
  let count = 0
  for (const node of tree) {
    count++
    if (node.children) {
      count += countCategories(node.children as Array<{ children?: unknown[] }>)
    }
  }
  return count
}

onMounted(async () => {
  // Load stats in parallel
  const [articlesRes, categoriesRes] = await Promise.all([
    articleApi.list({ page: 0, size: 1 }).catch(() => null),
    categoryApi.tree().catch(() => null),
  ])

  if (articlesRes) {
    stats.value.totalArticles = articlesRes.data.totalElements
  }

  if (categoriesRes) {
    stats.value.totalCategories = countCategories(categoriesRes.data)
  }

  // Get published/draft counts
  const [publishedRes, draftRes] = await Promise.all([
    articleApi.list({ page: 0, size: 1, status: 'PUBLISHED' }).catch(() => null),
    articleApi.list({ page: 0, size: 1, status: 'DRAFT' }).catch(() => null),
  ])

  if (publishedRes) {
    stats.value.publishedCount = publishedRes.data.totalElements
  }
  if (draftRes) {
    stats.value.draftCount = draftRes.data.totalElements
  }

  // Load recent logs
  logsLoading.value = true
  try {
    const { data } = await logApi.list({ page: 0, size: 10 })
    recentLogs.value = data.content
  } catch {
    // ignore
  } finally {
    logsLoading.value = false
  }
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

.page-title {
  margin-bottom: 24px;
  font-size: 22px;
  color: #303133;
}

.stats-row {
  margin-bottom: 0;
}

.stat-card {
  text-align: center;
}

.stat-number {
  font-size: 32px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.stat-number.published {
  color: #67c23a;
}

.stat-number.draft {
  color: #909399;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-actions .el-button {
  width: 100%;
}
</style>
