<template>
  <div class="setup-page">
    <div class="setup-card">
      <h2 class="setup-title">初始化管理员账户</h2>
      <p class="setup-desc">首次使用，请创建管理员账户</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        @submit.prevent="handleSetup"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="form.displayName" placeholder="请输入显示名称" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSetup" style="width: 100%">
            创建管理员账户
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = ref({
  username: '',
  displayName: '',
  password: '',
  confirmPassword: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度 3-50 字符', trigger: 'blur' },
  ],
  displayName: [
    { required: true, message: '请输入显示名称', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.value.password) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

onMounted(async () => {
  try {
    const { data } = await authApi.needSetup()
    if (!data.needSetup) {
      router.replace('/admin/login')
    }
  } catch {
    // ignore
  }
})

async function handleSetup() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const { data } = await authApi.setup({
      username: form.value.username,
      password: form.value.password,
      displayName: form.value.displayName,
    })
    authStore.setAuth(data.token, data.user)
    ElMessage.success('管理员账户创建成功')
    router.push('/admin')
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.setup-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.setup-card {
  width: 480px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.setup-title {
  text-align: center;
  margin-bottom: 8px;
  color: #303133;
  font-size: 24px;
}

.setup-desc {
  text-align: center;
  color: #909399;
  margin-bottom: 32px;
}
</style>
