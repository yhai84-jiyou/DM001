<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../api/request'
import { useDictionaryStore } from '../../stores/dictionary'

const router = useRouter()
const dict = useDictionaryStore()
const issues = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(50)
const loading = ref(false)

const filters = ref({
  platform_id: '', issue_type_id: '', status_id: '', priority_id: '', keyword: '',
})

const platforms = ref<any[]>([])
const issueTypes = ref<any[]>([])
const statuses = ref<any[]>([])
const priorities = ref<any[]>([])

async function loadDicts() {
  platforms.value = await dict.loadCategory('PLATFORM')
  issueTypes.value = await dict.loadCategory('ISSUE_TYPE')
  statuses.value = await dict.loadCategory('ISSUE_STATUS')
  priorities.value = await dict.loadCategory('ISSUE_PRIORITY')
}

async function loadIssues() {
  loading.value = true
  try {
    const params: any = { page: page.value, page_size: pageSize.value }
    for (const [k, v] of Object.entries(filters.value)) {
      if (v) params[k] = v
    }
    const res: any = await request.get('/issues', { params })
    issues.value = res.data.items
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function goDetail(params: { row: any }) {
  router.push(`/issues/${params.row.id}`)
}

async function handleCellEdit(params: { row: any; column: any }) {
  const { row, column } = params
  const field = column.field
  if (!field || field === 'issue_no' || field === 'submitted_at') return

  const payload: any = {}
  if (field === 'title') {
    payload.title = row.title
  }
  try {
    await request.put(`/issues/${row.id}`, payload)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.detail || '更新失败')
    loadIssues()
  }
}

function handlePageChange(params: { currentPage: number; pageSize: number }) {
  page.value = params.currentPage
  pageSize.value = params.pageSize
  loadIssues()
}

onMounted(async () => {
  await loadDicts()
  await loadIssues()
})
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 12px; flex-wrap: wrap; gap: 8px">
      <div style="display: flex; gap: 8px; flex-wrap: wrap">
        <el-select v-model="filters.platform_id" placeholder="平台" clearable @change="loadIssues" style="width: 160px" size="small">
          <el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-select v-model="filters.issue_type_id" placeholder="问题类型" clearable @change="loadIssues" style="width: 130px" size="small">
          <el-option v-for="t in issueTypes" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <el-select v-model="filters.status_id" placeholder="状态" clearable @change="loadIssues" style="width: 160px" size="small">
          <el-option v-for="s in statuses" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-select v-model="filters.priority_id" placeholder="优先级" clearable @change="loadIssues" style="width: 110px" size="small">
          <el-option v-for="p in priorities" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="搜索标题" clearable @clear="loadIssues" @keyup.enter="loadIssues" style="width: 180px" size="small">
          <template #append><el-button @click="loadIssues" icon="Search" /></template>
        </el-input>
      </div>
      <el-button type="primary" size="small" @click="router.push('/issues/new')">+ 新建问题</el-button>
    </div>

    <vxe-table
      :data="issues"
      :loading="loading"
      stripe
      border
      show-overflow
      :row-config="{ isHover: true, isCurrent: true }"
      :column-config="{ resizable: true }"
      :sort-config="{ trigger: 'cell', remote: true }"
      :edit-config="{ trigger: 'dblclick', mode: 'cell', showStatus: true }"
      height="calc(100vh - 230px)"
      @cell-dblclick="goDetail"
      @edit-closed="handleCellEdit"
    >
      <vxe-column field="issue_no" title="编号" width="170" :edit-render="{}" fixed="left">
        <template #default="{ row }">
          <a style="color: #409eff; cursor: pointer" @click.stop="router.push(`/issues/${row.id}`)">{{ row.issue_no }}</a>
        </template>
      </vxe-column>

      <vxe-column field="title" title="标题" min-width="250" :edit-render="{ autofocus: '.vxe-input--inner' }">
        <template #edit="{ row }">
          <vxe-input v-model="row.title" type="text" />
        </template>
      </vxe-column>

      <vxe-column field="platform" title="平台" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.platform" :color="row.platform.color" style="color: #fff" size="small" effect="dark">{{ row.platform.name }}</el-tag>
        </template>
      </vxe-column>

      <vxe-column field="issue_type" title="类型" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.issue_type" :color="row.issue_type.color" style="color: #fff" size="small" effect="dark">{{ row.issue_type.name }}</el-tag>
        </template>
      </vxe-column>

      <vxe-column field="status" title="状态" width="150">
        <template #default="{ row }">
          <el-tag v-if="row.status" :color="row.status.color" style="color: #fff" size="small" effect="dark">{{ row.status.name }}</el-tag>
        </template>
      </vxe-column>

      <vxe-column field="priority" title="优先级" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.priority" :color="row.priority.color" style="color: #fff" size="small" effect="dark">{{ row.priority.name }}</el-tag>
        </template>
      </vxe-column>

      <vxe-column title="提交人" width="80">
        <template #default="{ row }">{{ row.submitter?.name }}</template>
      </vxe-column>

      <vxe-column title="产品跟进" width="80">
        <template #default="{ row }">{{ row.product_owner?.name || '-' }}</template>
      </vxe-column>

      <vxe-column title="研发跟进" width="80">
        <template #default="{ row }">{{ row.dev_owner?.name || '-' }}</template>
      </vxe-column>

      <vxe-column title="测试跟进" width="80">
        <template #default="{ row }">{{ row.test_owner?.name || '-' }}</template>
      </vxe-column>

      <vxe-column title="提交时间" width="170" sortable>
        <template #default="{ row }">{{ row.submitted_at?.replace('T', ' ').slice(0, 19) }}</template>
      </vxe-column>

      <vxe-column title="最后更新" width="170" sortable>
        <template #default="{ row }">{{ row.updated_at?.replace('T', ' ').slice(0, 19) }}</template>
      </vxe-column>
    </vxe-table>

    <vxe-pager
      style="margin-top: 8px"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      :layouts="['Total', 'PrevPage', 'JumpNumber', 'NextPage', 'Sizes']"
      :page-sizes="[20, 50, 100]"
      @page-change="handlePageChange"
    />
  </div>
</template>
