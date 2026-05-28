<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../api/request'
import { useDictionaryStore } from '../../stores/dictionary'

const router = useRouter()
const dict = useDictionaryStore()
const issues = ref<any[]>([])
const total = ref(0)
const page = ref(1)
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
    const params: any = { page: page.value, page_size: 20 }
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

function goDetail(row: any) {
  router.push(`/issues/${row.id}`)
}

onMounted(async () => {
  await loadDicts()
  await loadIssues()
})
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 8px">
      <div style="display: flex; gap: 8px; flex-wrap: wrap">
        <el-select v-model="filters.platform_id" placeholder="平台" clearable @change="loadIssues" style="width: 160px">
          <el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-select v-model="filters.issue_type_id" placeholder="问题类型" clearable @change="loadIssues" style="width: 130px">
          <el-option v-for="t in issueTypes" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <el-select v-model="filters.status_id" placeholder="状态" clearable @change="loadIssues" style="width: 160px">
          <el-option v-for="s in statuses" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-select v-model="filters.priority_id" placeholder="优先级" clearable @change="loadIssues" style="width: 110px">
          <el-option v-for="p in priorities" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="搜索标题" clearable @clear="loadIssues" @keyup.enter="loadIssues" style="width: 180px">
          <template #append><el-button @click="loadIssues" icon="Search" /></template>
        </el-input>
      </div>
      <el-button type="primary" @click="router.push('/issues/new')">新建问题</el-button>
    </div>

    <el-table :data="issues" v-loading="loading" stripe @row-click="goDetail" style="cursor: pointer">
      <el-table-column prop="issue_no" label="编号" width="170" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="平台" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.platform" :color="row.platform.color" style="color: #fff" size="small">{{ row.platform.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ row.issue_type?.name }}</template>
      </el-table-column>
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.status" :color="row.status.color" style="color: #fff" size="small">{{ row.status.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.priority" :color="row.priority.color" style="color: #fff" size="small">{{ row.priority.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交人" width="80">
        <template #default="{ row }">{{ row.submitter?.name }}</template>
      </el-table-column>
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ row.submitted_at?.replace('T', ' ').slice(0, 19) }}</template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 16px; justify-content: flex-end"
      :current-page="page" :total="total" :page-size="20"
      layout="total, prev, pager, next" @current-change="(p: number) => { page = p; loadIssues() }"
    />
  </div>
</template>
