<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'

const users = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const filterStatus = ref('')
const filterType = ref('')

async function loadUsers() {
  loading.value = true
  try {
    const params: any = { page: page.value, page_size: 20 }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterType.value) params.user_type = filterType.value
    const res: any = await request.get('/users', { params })
    users.value = res.data.items
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function approveUser(userId: string) {
  await ElMessageBox.confirm('确定审批通过该用户？', '确认')
  await request.post(`/users/${userId}/approve`)
  ElMessage.success('已审批通过')
  loadUsers()
}

async function disableUser(userId: string) {
  await ElMessageBox.confirm('确定禁用该用户？', '确认')
  await request.post(`/users/${userId}/disable`)
  ElMessage.success('已禁用')
  loadUsers()
}

function statusTag(status: string) {
  return { ACTIVE: 'success', PENDING: 'warning', DISABLED: 'danger' }[status] || 'info'
}

onMounted(loadUsers)
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <div style="display: flex; gap: 12px">
        <el-select v-model="filterStatus" placeholder="状态" clearable @change="loadUsers" style="width: 120px">
          <el-option label="待审批" value="PENDING" />
          <el-option label="已启用" value="ACTIVE" />
          <el-option label="已禁用" value="DISABLED" />
        </el-select>
        <el-select v-model="filterType" placeholder="用户类型" clearable @change="loadUsers" style="width: 120px">
          <el-option label="甲方" value="CLIENT" />
          <el-option label="乙方" value="VENDOR" />
        </el-select>
      </div>
    </div>

    <el-table :data="users" v-loading="loading" stripe>
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="org_name" label="组织" width="140" />
      <el-table-column prop="user_type" label="类型" width="80">
        <template #default="{ row }">{{ row.user_type === 'CLIENT' ? '甲方' : '乙方' }}</template>
      </el-table-column>
      <el-table-column prop="vendor_role" label="角色" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">
            {{ ({ ACTIVE: '已启用', PENDING: '待审批', DISABLED: '已禁用' } as Record<string, string>)[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="160">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r.code" size="small" style="margin-right: 4px">{{ r.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" type="primary" size="small" @click="approveUser(row.id)">审批</el-button>
          <el-button v-if="row.status === 'ACTIVE'" type="danger" size="small" @click="disableUser(row.id)">禁用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 16px; justify-content: flex-end"
      :current-page="page" :total="total" :page-size="20"
      layout="total, prev, pager, next" @current-change="(p: number) => { page = p; loadUsers() }"
    />
  </div>
</template>
