<template>
  <div class="change-password-page">
    <div class="change-password-card">
      <h2 class="page-title">修改密码</h2>
      <p class="page-desc">首次登录或密码已被重置，请修改密码后继续使用</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        @submit.prevent="handleChange"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" placeholder="请输入旧密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleChange" style="width: 100%">
            修改密码
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { profileApi } from '@/api/profile'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.value.newPassword) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function handleChange() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await profileApi.changePassword({
      oldPassword: form.value.oldPassword,
      newPassword: form.value.newPassword,
    })
    if (authStore.user) {
      authStore.setUser({ ...authStore.user, forcePasswordChange: false })
    }
    ElMessage.success('密码修改成功')
    router.push('/admin')
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.change-password-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.change-password-card {
  width: 480px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.page-title {
  text-align: center;
  margin-bottom: 8px;
  color: #303133;
  font-size: 24px;
}

.page-desc {
  text-align: center;
  color: #909399;
  margin-bottom: 32px;
}
</style>
