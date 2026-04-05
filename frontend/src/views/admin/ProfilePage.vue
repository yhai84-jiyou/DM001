<template>
  <div class="profile-page">
    <h2 class="page-title">个人设置</h2>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>基本信息</span>
          </template>
          <el-form :model="profileForm" label-width="80px">
            <el-form-item label="用户名">
              <el-input :model-value="authStore.user?.username" disabled />
            </el-form-item>
            <el-form-item label="角色">
              <el-input :model-value="authStore.user?.role === 'ADMIN' ? '管理员' : '编辑者'" disabled />
            </el-form-item>
            <el-form-item label="显示名称">
              <el-input v-model="profileForm.displayName" placeholder="请输入显示名称" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileSaving" @click="handleUpdateProfile">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>修改密码</span>
          </template>
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordSaving" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { profileApi } from '@/api/profile'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

// Profile
const profileForm = ref({ displayName: '' })
const profileSaving = ref(false)

// Password
const passwordFormRef = ref<FormInstance>()
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordSaving = ref(false)

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function handleUpdateProfile() {
  if (!profileForm.value.displayName) {
    ElMessage.warning('请输入显示名称')
    return
  }
  profileSaving.value = true
  try {
    await profileApi.update({ displayName: profileForm.value.displayName })
    if (authStore.user) {
      authStore.setUser({ ...authStore.user, displayName: profileForm.value.displayName })
    }
    ElMessage.success('保存成功')
  } catch {
    // Error handled by interceptor
  } finally {
    profileSaving.value = false
  }
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  passwordSaving.value = true
  try {
    await profileApi.changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword,
    })
    ElMessage.success('密码修改成功')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch {
    // Error handled by interceptor
  } finally {
    passwordSaving.value = false
  }
}

onMounted(() => {
  profileForm.value.displayName = authStore.user?.displayName || ''
})
</script>

<style scoped>
.page-title {
  margin-bottom: 20px;
  font-size: 20px;
  color: #303133;
}
</style>
