<template>
  <div class="doc-layout">
    <!-- Top header bar -->
    <header class="doc-header">
      <div class="doc-header-left">
        <button class="hamburger-btn" @click="sidebarOpen = !sidebarOpen" v-if="isMobile">
          <el-icon :size="20"><Menu /></el-icon>
        </button>
        <router-link to="/" class="doc-logo">帮助手册</router-link>
      </div>
      <div class="doc-header-right">
        <div class="search-trigger" @click="showSearch = true">
          <el-icon><Search /></el-icon>
          <span class="search-text">搜索文档...</span>
          <kbd v-if="!isMobile">Ctrl+K</kbd>
        </div>
        <router-link to="/admin" class="admin-link">管理后台</router-link>
      </div>
    </header>

    <!-- Sidebar overlay for mobile -->
    <div class="sidebar-overlay" v-if="sidebarOpen && isMobile" @click="sidebarOpen = false"></div>

    <div class="doc-body">
      <!-- Left sidebar -->
      <aside class="doc-sidebar" :class="{ open: sidebarOpen }">
        <SidebarTree :categories="categoryStore.publicTree" @select="onArticleSelect" />
      </aside>

      <!-- Main content -->
      <main class="doc-main">
        <router-view />
      </main>
    </div>

    <!-- Search dialog -->
    <SearchDialog v-model="showSearch" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Search, Menu } from '@element-plus/icons-vue'
import { useCategoryStore } from '@/stores/category'
import SidebarTree from '@/components/doc/SidebarTree.vue'
import SearchDialog from '@/components/doc/SearchDialog.vue'
import type { PublicArticle } from '@/types'

const categoryStore = useCategoryStore()

const showSearch = ref(false)
const sidebarOpen = ref(false)
const isMobile = ref(false)

function checkMobile() {
  isMobile.value = window.innerWidth < 768
  if (!isMobile.value) {
    sidebarOpen.value = false
  }
}

function onKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    showSearch.value = true
  }
}

function onArticleSelect(_article: PublicArticle) {
  if (isMobile.value) {
    sidebarOpen.value = false
  }
}

onMounted(() => {
  categoryStore.fetchPublicTree()
  checkMobile()
  window.addEventListener('resize', checkMobile)
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  window.removeEventListener('keydown', onKeydown)
})

// Expose showSearch for child components (HomePage hero search)
defineExpose({ showSearch })
</script>

<style lang="scss" scoped>
.doc-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.doc-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--doc-header-height);
  padding: 0 24px;
  background: var(--doc-bg);
  border-bottom: 1px solid var(--doc-border);
}

.doc-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hamburger-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: none;
  cursor: pointer;
  border-radius: 4px;
  color: var(--doc-text);

  &:hover {
    background: #f3f4f6;
  }
}

.doc-logo {
  font-size: 18px;
  font-weight: 700;
  color: var(--doc-text);
  text-decoration: none;

  &:hover {
    text-decoration: none;
  }
}

.doc-header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border: 1px solid var(--doc-border);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: var(--doc-text-secondary);
  background: #f9fafb;
  transition: border-color 0.2s, box-shadow 0.2s;

  &:hover {
    border-color: var(--doc-primary);
    box-shadow: 0 0 0 1px var(--doc-primary);
  }

  kbd {
    font-size: 11px;
    padding: 1px 5px;
    background: #e5e7eb;
    border: 1px solid #d1d5db;
    border-radius: 3px;
    font-family: inherit;
  }
}

.search-text {
  @media (max-width: 500px) {
    display: none;
  }
}

.admin-link {
  font-size: 14px;
  color: var(--doc-text-secondary);
  text-decoration: none;
  white-space: nowrap;

  &:hover {
    color: var(--doc-primary);
    text-decoration: none;
  }
}

.sidebar-overlay {
  position: fixed;
  inset: 0;
  top: var(--doc-header-height);
  background: rgba(0, 0, 0, 0.3);
  z-index: 50;
}

.doc-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.doc-sidebar {
  width: var(--doc-sidebar-width);
  flex-shrink: 0;
  border-right: 1px solid var(--doc-border);
  overflow-y: auto;
  height: calc(100vh - var(--doc-header-height));
  position: sticky;
  top: var(--doc-header-height);

  @media (max-width: 767px) {
    position: fixed;
    top: var(--doc-header-height);
    left: 0;
    bottom: 0;
    z-index: 60;
    background: var(--doc-bg);
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    box-shadow: none;

    &.open {
      transform: translateX(0);
      box-shadow: 4px 0 12px rgba(0, 0, 0, 0.1);
    }
  }
}

.doc-main {
  flex: 1;
  min-width: 0;
  padding: 24px 32px;

  @media (max-width: 767px) {
    padding: 16px;
  }
}
</style>
