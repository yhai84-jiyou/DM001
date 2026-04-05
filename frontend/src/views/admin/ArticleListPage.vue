<template>
  <div class="article-list-page">
    <div class="page-header">
      <h2>文章管理</h2>
      <div class="header-actions">
        <el-button @click="$router.push('/admin/articles/trash')">回收站</el-button>
        <el-button type="primary" :icon="Plus" @click="$router.push('/admin/articles/new')">新建文章</el-button>
      </div>
    </div>

    <el-card>
      <div class="filter-bar">
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 140px" @change="loadArticles">
          <el-option label="全部" value="" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
        </el-select>
        <el-select v-model="filters.categoryId" placeholder="分类" clearable style="width: 180px" @change="loadArticles">
          <el-option
            v-for="cat in flatCategories"
            :key="cat.id"
            :label="cat.label"
            :value="cat.id"
          />
        </el-select>
        <el-input
          v-model="filters.search"
          placeholder="搜索文章标题..."
          clearable
          style="width: 240px"
          @clear="loadArticles"
          @keyup.enter="loadArticles"
        >
          <template #append>
            <el-button :icon="Search" @click="loadArticles" />
          </template>
        </el-input>
      </div>

      <el-table :data="articles" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/admin/articles/${row.id}/edit`)">
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="150">
          <template #default="{ row }">
            {{ getCategoryName(row.categoryId) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentVersion" label="版本" width="90" align="center">
          <template #default="{ row }">
            {{ row.currentVersion || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/admin/articles/${row.id}/edit`)">
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              link
              type="success"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-else
              link
              type="warning"
              @click="handleUnpublish(row)"
            >
              取消发布
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
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

    <!-- Publish Dialog -->
    <el-dialog v-model="publishDialogVisible" title="发布文章" width="480px">
      <el-form :model="publishForm" label-width="80px">
        <el-form-item label="版本号">
          <el-input v-model="publishForm.versionLabel" placeholder="例如 v1.0" />
        </el-form-item>
        <el-form-item label="更新说明">
          <el-input
            v-model="publishForm.changeNotes"
            type="textarea"
            :rows="3"
            placeholder="请填写更新说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishLoading" @click="confirmPublish">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { articleApi } from '@/api/article'
import { useCategoryStore } from '@/stores/category'
import type { Article, Category } from '@/types'

const categoryStore = useCategoryStore()

const articles = ref<Article[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filters = ref({
  status: '',
  categoryId: undefined as number | undefined,
  search: '',
})

const flatCategories = ref<Array<{ id: number; label: string }>>([])

const publishDialogVisible = ref(false)
const publishLoading = ref(false)
const publishForm = ref({ versionLabel: '', changeNotes: '' })
const publishTargetId = ref<number | null>(null)

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

function flattenCategories(cats: Category[], prefix = ''): Array<{ id: number; label: string }> {
  const result: Array<{ id: number; label: string }> = []
  for (const cat of cats) {
    result.push({ id: cat.id, label: prefix + cat.name })
    if (cat.children?.length) {
      result.push(...flattenCategories(cat.children, prefix + cat.name + ' / '))
    }
  }
  return result
}

function getCategoryName(categoryId: number): string {
  const cat = categoryStore.findCategory(categoryId, categoryStore.adminTree)
  return cat?.name || '-'
}

async function loadArticles() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: currentPage.value - 1,
      size: pageSize.value,
    }
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.categoryId) params.categoryId = filters.value.categoryId
    if (filters.value.search) params.search = filters.value.search

    const { data } = await articleApi.list(params as Parameters<typeof articleApi.list>[0])
    articles.value = data.content
    total.value = data.totalElements
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handlePublish(row: Article) {
  publishTargetId.value = row.id
  publishForm.value = { versionLabel: '', changeNotes: '' }
  publishDialogVisible.value = true
}

async function confirmPublish() {
  if (!publishTargetId.value) return
  if (!publishForm.value.versionLabel) {
    ElMessage.warning('请输入版本号')
    return
  }
  publishLoading.value = true
  try {
    await articleApi.publish(publishTargetId.value, publishForm.value)
    ElMessage.success('发布成功')
    publishDialogVisible.value = false
    loadArticles()
  } catch {
    // Error handled by interceptor
  } finally {
    publishLoading.value = false
  }
}

async function handleUnpublish(row: Article) {
  try {
    await ElMessageBox.confirm('确定取消发布该文章？', '提示', { type: 'warning' })
    await articleApi.unpublish(row.id)
    ElMessage.success('已取消发布')
    loadArticles()
  } catch {
    // cancelled or error
  }
}

async function handleDelete(row: Article) {
  try {
    await ElMessageBox.confirm('确定将该文章移到回收站？', '提示', { type: 'warning' })
    await articleApi.delete(row.id)
    ElMessage.success('已移到回收站')
    loadArticles()
  } catch {
    // cancelled or error
  }
}

onMounted(async () => {
  await categoryStore.fetchAdminTree()
  flatCategories.value = flattenCategories(categoryStore.adminTree)
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
