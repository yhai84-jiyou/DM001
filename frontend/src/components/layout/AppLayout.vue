<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()

function handleLogout() {
  auth.logout()
  router.push('/login')
}

const menuItems = [
  { path: '/dashboard', title: '总览看板', icon: 'DataAnalysis' },
  { path: '/issues', title: '问题管理', icon: 'List' },
  { path: '/reports', title: '日报周报', icon: 'Document' },
]

const adminItems = [
  { path: '/admin/users', title: '用户管理', icon: 'User' },
  { path: '/admin/org', title: '组织管理', icon: 'OfficeBuilding' },
  { path: '/admin/roles', title: '角色权限', icon: 'Lock' },
  { path: '/admin/dict', title: '数据字典', icon: 'Setting' },
]
</script>

<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #304156">
      <div style="padding: 20px; text-align: center; color: #fff; font-size: 16px; font-weight: bold">
        问题管理系统
      </div>
      <el-menu
        :default-active="$route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>

        <el-sub-menu index="admin">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item v-for="item in adminItems" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="display: flex; align-items: center; justify-content: flex-end; border-bottom: 1px solid #e6e6e6">
        <span style="margin-right: 16px">{{ auth.user?.name || '未登录' }}</span>
        <el-button type="text" @click="handleLogout">退出</el-button>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
