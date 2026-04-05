<template>
  <div class="admin-layout">
    <el-container style="min-height: 100vh;">
      <el-header class="admin-header">
        <div class="header-left">
          <span class="logo">帮助手册管理</span>
        </div>
        <div class="header-right">
          <span class="user-name">{{ authStore.user?.displayName }}</span>
          <el-button text @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-container>
        <el-aside width="200px" class="admin-aside">
          <el-menu
            :default-active="activeMenu"
            router
            class="admin-menu"
          >
            <el-menu-item index="/admin">
              <el-icon><Odometer /></el-icon>
              <span>仪表盘</span>
            </el-menu-item>
            <el-menu-item index="/admin/articles">
              <el-icon><Document /></el-icon>
              <span>文章管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/categories">
              <el-icon><Folder /></el-icon>
              <span>分类管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/visibility">
              <el-icon><View /></el-icon>
              <span>可见性</span>
            </el-menu-item>
            <el-menu-item index="/admin/media">
              <el-icon><Picture /></el-icon>
              <span>媒体管理</span>
            </el-menu-item>
            <el-menu-item v-if="authStore.isAdmin" index="/admin/users">
              <el-icon><UserFilled /></el-icon>
              <span>用户管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/logs">
              <el-icon><List /></el-icon>
              <span>操作日志</span>
            </el-menu-item>
            <el-divider />
            <el-menu-item index="/admin/profile">
              <el-icon><Setting /></el-icon>
              <span>个人设置</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Odometer,
  Document,
  Folder,
  View,
  Picture,
  UserFilled,
  List,
  Setting,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/admin/articles')) return '/admin/articles'
  if (path.startsWith('/admin/categories')) return '/admin/categories'
  if (path === '/admin' || path === '/admin/') return '/admin'
  return path
})

async function handleLogout() {
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/admin/login')
}
</script>

<style scoped>
.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  height: 56px;
}

.header-left .logo {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  color: #606266;
  font-size: 14px;
}

.admin-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
}

.admin-menu {
  border-right: none;
}

.admin-main {
  background: #f5f7fa;
  min-height: calc(100vh - 56px);
}
</style>
