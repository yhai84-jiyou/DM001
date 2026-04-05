<template>
  <div class="search-page">
    <div class="search-page-header">
      <h1>搜索结果</h1>
      <div class="search-input-wrap">
        <el-input
          v-model="keyword"
          placeholder="搜索文档..."
          size="large"
          :prefix-icon="Search"
          clearable
          @input="onSearchDebounced"
          @keydown.enter="doSearch"
        />
      </div>
      <p class="search-summary" v-if="keyword && !loading">
        共找到 {{ total }} 条结果
      </p>
    </div>

    <div class="search-results" v-if="results.length">
      <div
        v-for="item in results"
        :key="item.id"
        class="search-result-item"
        @click="$router.push(`/doc/${item.slug}`)"
      >
        <h3 class="result-title">{{ item.title }}</h3>
        <p class="result-summary" v-if="item.summary">{{ item.summary }}</p>
        <div class="result-meta" v-if="item.publishedAt">
          最后更新 {{ formatDate(item.publishedAt) }}
        </div>
      </div>
    </div>

    <div class="search-empty" v-else-if="keyword && !loading">
      <el-empty description="没有找到相关文档" />
    </div>

    <div class="search-loading" v-if="loading">
      <el-skeleton :rows="6" animated />
    </div>

    <div class="search-pagination" v-if="totalPages > 1">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="doSearch"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { articleApi } from '@/api/article'
import type { PublicArticle } from '@/types'

const route = useRoute()
const router = useRouter()

const keyword = ref((route.query.q as string) || '')
const results = ref<PublicArticle[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const totalPages = computed(() => Math.ceil(total.value / pageSize))

let debounceTimer: ReturnType<typeof setTimeout> | null = null

function onSearchDebounced() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    currentPage.value = 1
    doSearch()
  }, 300)
}

async function doSearch() {
  if (!keyword.value.trim()) {
    results.value = []
    total.value = 0
    return
  }

  loading.value = true
  try {
    router.replace({ query: { q: keyword.value } })
    const { data } = await articleApi.search({
      q: keyword.value.trim(),
      page: currentPage.value - 1,
      size: pageSize,
    })
    results.value = data.content
    total.value = data.totalElements
  } catch {
    results.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

onMounted(() => {
  if (keyword.value) {
    doSearch()
  }
})
</script>

<style lang="scss" scoped>
.search-page {
  max-width: var(--doc-content-max-width);
  margin: 0 auto;
  padding: 24px 0 48px;
}

.search-page-header {
  margin-bottom: 32px;

  h1 {
    font-size: 1.8em;
    font-weight: 700;
    margin-bottom: 16px;
  }
}

.search-input-wrap {
  max-width: 480px;
  margin-bottom: 12px;
}

.search-summary {
  font-size: 14px;
  color: var(--doc-text-secondary);
}

.search-results {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.search-result-item {
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: #f9fafb;
  }
}

.result-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--doc-text);
  margin-bottom: 4px;
}

.result-summary {
  font-size: 14px;
  color: var(--doc-text-secondary);
  line-height: 1.6;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-meta {
  font-size: 12px;
  color: #9ca3af;
}

.search-empty {
  padding: 48px 0;
}

.search-loading {
  padding: 24px 0;
}

.search-pagination {
  display: flex;
  justify-content: center;
  padding-top: 32px;
}
</style>
