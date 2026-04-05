<template>
  <div class="home-page">
    <div class="hero-section">
      <h1>帮助手册</h1>
      <p class="hero-desc">查找您需要的文档和教程</p>
      <div class="hero-search" @click="openSearch">
        <el-icon><Search /></el-icon>
        <span>搜索文档...</span>
      </div>
    </div>
    <div class="category-cards" v-if="topCategories.length">
      <div
        v-for="cat in topCategories"
        :key="cat.id"
        class="category-card"
        @click="navigateToCategory(cat)"
      >
        <div class="card-icon">{{ cat.icon || '\uD83D\uDCC1' }}</div>
        <h3>{{ cat.name }}</h3>
        <p class="card-desc">{{ articleCounts[cat.id] ?? 0 }} 篇文章</p>
      </div>
    </div>
    <div class="home-empty" v-else-if="!categoryStore.loading">
      <el-empty description="暂无文档" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useCategoryStore } from '@/stores/category'
import { articleApi } from '@/api/article'
import type { Category } from '@/types'

const categoryStore = useCategoryStore()
const router = useRouter()

const topCategories = computed(() => categoryStore.publicTree)
const articleCounts = ref<Record<number, number>>({})
const firstSlugs = ref<Record<number, string>>({})

async function loadCategoryArticles() {
  for (const cat of topCategories.value) {
    try {
      const { data } = await articleApi.listByCategory(cat.id)
      articleCounts.value[cat.id] = countAllArticles(cat, data.length)
      if (data.length > 0) {
        firstSlugs.value[cat.id] = data[0].slug
      }
    } catch {
      articleCounts.value[cat.id] = 0
    }
  }
}

function countAllArticles(cat: Category, directCount: number): number {
  let total = directCount
  if (cat.children) {
    total += cat.children.length // Approximate; we don't fetch all nested
  }
  return total
}

function navigateToCategory(cat: Category) {
  const slug = firstSlugs.value[cat.id]
  if (slug) {
    router.push(`/doc/${slug}`)
  }
}

function openSearch() {
  const instance = getCurrentInstance()
  const parent = instance?.parent
  if (parent?.exposed?.showSearch !== undefined) {
    parent.exposed.showSearch.value = true
  }
}

onMounted(() => {
  // Wait for categories to load then fetch article counts
  if (topCategories.value.length > 0) {
    loadCategoryArticles()
  } else {
    const stop = setInterval(() => {
      if (topCategories.value.length > 0) {
        clearInterval(stop)
        loadCategoryArticles()
      }
    }, 200)
    // Safety: stop after 10s
    setTimeout(() => clearInterval(stop), 10000)
  }
})
</script>

<style lang="scss" scoped>
.home-page {
  max-width: 960px;
  margin: 0 auto;
}

.hero-section {
  text-align: center;
  padding: 64px 0 48px;

  h1 {
    font-size: 2.5em;
    font-weight: 700;
    color: var(--doc-text);
    margin-bottom: 8px;
  }

  .hero-desc {
    font-size: 18px;
    color: var(--doc-text-secondary);
    margin-bottom: 32px;
  }
}

.hero-search {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 12px 24px;
  border: 1px solid var(--doc-border);
  border-radius: 12px;
  cursor: pointer;
  font-size: 16px;
  color: var(--doc-text-secondary);
  background: #f9fafb;
  min-width: 300px;
  transition: border-color 0.2s, box-shadow 0.2s;

  &:hover {
    border-color: var(--doc-primary);
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
  }
}

.category-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
  padding-bottom: 48px;
}

.category-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 20px;
  border: 1px solid var(--doc-border);
  border-radius: 12px;
  cursor: pointer;
  text-decoration: none;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
  background: var(--doc-bg);

  &:hover {
    border-color: var(--doc-primary);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
    transform: translateY(-2px);
    text-decoration: none;
  }

  .card-icon {
    font-size: 36px;
    margin-bottom: 12px;
  }

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: var(--doc-text);
    margin-bottom: 6px;
  }

  .card-desc {
    font-size: 13px;
    color: var(--doc-text-secondary);
  }
}

.home-empty {
  padding: 80px 0;
}
</style>
