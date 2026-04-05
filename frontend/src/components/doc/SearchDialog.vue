<template>
  <el-dialog
    v-model="visible"
    :show-close="false"
    class="search-dialog"
    width="600px"
    top="20vh"
    append-to-body
    @opened="onOpened"
  >
    <template #header>
      <div class="search-header">
        <el-icon class="search-icon"><Search /></el-icon>
        <input
          ref="inputRef"
          v-model="keyword"
          placeholder="搜索文档..."
          class="search-input"
          @input="onSearch"
          @keydown.down.prevent="moveDown"
          @keydown.up.prevent="moveUp"
          @keydown.enter.prevent="goToSelected"
          @keydown.esc="visible = false"
        />
      </div>
    </template>

    <div class="search-body">
      <div class="search-results" v-if="results.length">
        <div
          v-for="(item, index) in results"
          :key="item.id"
          class="search-item"
          :class="{ active: index === activeIndex }"
          @click="goTo(item)"
          @mouseenter="activeIndex = index"
        >
          <div class="search-item-title">{{ item.title }}</div>
          <div class="search-item-summary" v-html="highlightKeyword(item.summary || '')"></div>
        </div>
      </div>
      <div class="search-empty" v-else-if="keyword && !loading">
        没有找到相关文档
      </div>
      <div class="search-hint" v-else-if="!keyword">
        输入关键词搜索文档
      </div>
      <div class="search-loading" v-if="loading">
        搜索中...
      </div>
    </div>

    <template #footer>
      <div class="search-footer">
        <span><kbd>&uarr;</kbd><kbd>&darr;</kbd> 导航</span>
        <span><kbd>&crarr;</kbd> 打开</span>
        <span><kbd>Esc</kbd> 关闭</span>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import type { PublicArticle } from '@/types'
import { articleApi } from '@/api/article'

const visible = defineModel<boolean>({ default: false })

const router = useRouter()
const inputRef = ref<HTMLInputElement>()
const keyword = ref('')
const results = ref<PublicArticle[]>([])
const activeIndex = ref(0)
const loading = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

function onOpened() {
  inputRef.value?.focus()
}

function onSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  activeIndex.value = 0

  if (!keyword.value.trim()) {
    results.value = []
    loading.value = false
    return
  }

  loading.value = true
  searchTimer = setTimeout(async () => {
    try {
      const { data } = await articleApi.search({ q: keyword.value.trim(), size: 20 })
      results.value = data.content
    } catch {
      results.value = []
    } finally {
      loading.value = false
    }
  }, 300)
}

function moveDown() {
  if (activeIndex.value < results.value.length - 1) {
    activeIndex.value++
  }
}

function moveUp() {
  if (activeIndex.value > 0) {
    activeIndex.value--
  }
}

function goToSelected() {
  if (results.value.length > 0) {
    goTo(results.value[activeIndex.value])
  }
}

function goTo(item: PublicArticle) {
  visible.value = false
  router.push(`/doc/${item.slug}`)
}

function highlightKeyword(text: string): string {
  if (!keyword.value.trim() || !text) return text
  const escaped = keyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escaped})`, 'gi')
  return text.replace(regex, '<mark>$1</mark>')
}

// Reset on close
watch(visible, (val) => {
  if (!val) {
    keyword.value = ''
    results.value = []
    activeIndex.value = 0
  }
})
</script>

<style lang="scss">
.search-dialog {
  .el-dialog__header {
    padding: 0;
    margin: 0;
  }

  .el-dialog__body {
    padding: 0;
  }

  .el-dialog__footer {
    padding: 8px 16px;
  }
}
</style>

<style lang="scss" scoped>
.search-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--doc-border);
}

.search-icon {
  font-size: 18px;
  color: #9ca3af;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  background: transparent;
  color: var(--doc-text);

  &::placeholder {
    color: #9ca3af;
  }
}

.search-body {
  max-height: 400px;
  overflow-y: auto;
  min-height: 100px;
}

.search-results {
  padding: 8px;
}

.search-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover,
  &.active {
    background: #f3f4f6;
  }
}

.search-item-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--doc-text);
  margin-bottom: 2px;
}

.search-item-summary {
  font-size: 13px;
  color: var(--doc-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  :deep(mark) {
    background: #fef08a;
    color: inherit;
    padding: 0 1px;
    border-radius: 2px;
  }
}

.search-empty,
.search-hint,
.search-loading {
  padding: 32px 16px;
  text-align: center;
  color: var(--doc-text-secondary);
  font-size: 14px;
}

.search-footer {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--doc-text-secondary);

  kbd {
    display: inline-block;
    padding: 1px 5px;
    font-size: 11px;
    font-family: inherit;
    background: #f3f4f6;
    border: 1px solid #d1d5db;
    border-radius: 3px;
    margin: 0 2px;
  }
}
</style>
