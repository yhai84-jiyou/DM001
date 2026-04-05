<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="login-title">帮助手册管理系统</h2>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="UserIcon"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            style="width: 100%"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User as UserIcon, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = ref({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

onMounted(async () => {
  try {
    const { data } = await authApi.needSetup()
    if (data.needSetup) {
      router.replace('/admin/setup')
    }
  } catch {
    // ignore
  }
})

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const { data } = await authApi.login(form.value)
    authStore.setAuth(data.token, data.user)
    ElMessage.success('登录成功')

    if (data.user.forcePasswordChange) {
      router.push('/admin/change-password')
    } else {
      const redirect = (route.query.redirect as string) || '/admin'
      router.push(redirect)
    }
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.login-title {
  text-align: center;
  margin-bottom: 32px;
  color: #303133;
  font-size: 24px;
}
</style>
