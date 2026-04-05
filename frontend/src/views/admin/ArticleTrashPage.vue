<template>
  <div class="article-trash-page">
    <div class="page-header">
      <h2>回收站</h2>
      <el-button @click="$router.push('/admin/articles')">
        <el-icon><ArrowLeft /></el-icon>
        返回文章列表
      </el-button>
    </div>

    <el-card>
      <el-table :data="articles" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="删除时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleRestore(row)">恢复</el-button>
            <el-button link type="danger" @click="handlePermanentDelete(row)">彻底删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadArticles"
          @current-change="loadArticles"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { articleApi } from '@/api/article'
import type { Article } from '@/types'

const articles = ref<Article[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadArticles() {
  loading.value = true
  try {
    const { data } = await articleApi.trash({
      page: currentPage.value - 1,
      size: pageSize.value,
    })
    articles.value = data.content
    total.value = data.totalElements
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleRestore(row: Article) {
  try {
    await ElMessageBox.confirm('确定恢复该文章？', '提示', { type: 'info' })
    await articleApi.restore(row.id)
    ElMessage.success('已恢复')
    loadArticles()
  } catch {
    // cancelled or error
  }
}

async function handlePermanentDelete(row: Article) {
  try {
    await ElMessageBox.confirm('确定彻底删除该文章？此操作不可恢复！', '警告', { type: 'error' })
    await articleApi.permanentDelete(row.id)
    ElMessage.success('已彻底删除')
    loadArticles()
  } catch {
    // cancelled or error
  }
}

onMounted(() => {
  loadArticles()
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

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
