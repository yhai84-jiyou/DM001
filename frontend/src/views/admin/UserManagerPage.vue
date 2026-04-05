<template>
  <div class="user-manager-page">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog">新建用户</el-button>
    </div>

    <el-card>
      <el-table :data="users" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="displayName" label="显示名称" width="150" />
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : ''">
              {{ row.role === 'ADMIN' ? '管理员' : '编辑者' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleResetPassword(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Create User Dialog -->
    <el-dialog v-model="createDialogVisible" title="新建用户" width="480px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="createForm.displayName" placeholder="请输入显示名称" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role" style="width: 100%">
            <el-option label="编辑者" value="EDITOR" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Edit User Dialog -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="480px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="editForm.displayName" placeholder="请输入显示名称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="编辑者" value="EDITOR" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Generated Password Dialog -->
    <el-dialog v-model="passwordDialogVisible" title="用户密码" width="480px" :close-on-click-modal="false">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="请妥善保存此密码，关闭后将无法再次查看！"
        style="margin-bottom: 16px;"
      />
      <div class="password-display">
        <code>{{ generatedPassword }}</code>
        <el-button size="small" @click="copyPassword">复制</el-button>
      </div>
      <template #footer>
        <el-button type="primary" @click="passwordDialogVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { userApi } from '@/api/user'
import type { User } from '@/types'

const users = ref<User[]>([])
const loading = ref(false)

// Create
const createDialogVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = ref({ username: '', displayName: '', role: 'EDITOR' })
const createRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

// Edit
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editForm = ref({ id: 0, username: '', displayName: '', role: '' })

// Password
const passwordDialogVisible = ref(false)
const generatedPassword = ref('')

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadUsers() {
  loading.value = true
  try {
    const { data } = await userApi.list()
    users.value = data
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

function showCreateDialog() {
  createForm.value = { username: '', displayName: '', role: 'EDITOR' }
  createDialogVisible.value = true
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return

  createLoading.value = true
  try {
    const { data } = await userApi.create(createForm.value)
    generatedPassword.value = data.password
    createDialogVisible.value = false
    passwordDialogVisible.value = true
    loadUsers()
  } catch {
    // Error handled by interceptor
  } finally {
    createLoading.value = false
  }
}

function showEditDialog(user: User) {
  editForm.value = {
    id: user.id,
    username: user.username,
    displayName: user.displayName,
    role: user.role,
  }
  editDialogVisible.value = true
}

async function handleEdit() {
  editLoading.value = true
  try {
    await userApi.update(editForm.value.id, {
      displayName: editForm.value.displayName,
      role: editForm.value.role,
    })
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadUsers()
  } catch {
    // Error handled by interceptor
  } finally {
    editLoading.value = false
  }
}

async function handleToggleStatus(user: User) {
  const action = user.status === 'ACTIVE' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}用户「${user.displayName}」？`, '提示', { type: 'warning' })
    await userApi.toggleStatus(user.id)
    ElMessage.success(`已${action}`)
    loadUsers()
  } catch {
    // cancelled or error
  }
}

async function handleResetPassword(user: User) {
  try {
    await ElMessageBox.confirm(`确定重置用户「${user.displayName}」的密码？`, '提示', { type: 'warning' })
    const { data } = await userApi.resetPassword(user.id)
    generatedPassword.value = data.password
    passwordDialogVisible.value = true
  } catch {
    // cancelled or error
  }
}

function copyPassword() {
  navigator.clipboard.writeText(generatedPassword.value).then(() => {
    ElMessage.success('密码已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

onMounted(() => {
  loadUsers()
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

.password-display {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
}

.password-display code {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  word-break: break-all;
}
</style>
