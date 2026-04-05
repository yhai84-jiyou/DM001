<template>
  <div class="article-page" v-if="article">
    <!-- Breadcrumb -->
    <nav class="breadcrumb">
      <router-link to="/">首页</router-link>
      <template v-for="cat in breadcrumbs" :key="cat.id">
        <span class="separator">/</span>
        <span>{{ cat.name }}</span>
      </template>
      <span class="separator">/</span>
      <span class="current">{{ article.title }}</span>
    </nav>

    <div class="article-with-toc">
      <!-- Article content -->
      <article class="article-content" ref="contentRef">
        <h1>{{ article.title }}</h1>
        <div class="article-html" v-html="sanitizedContent"></div>
        <div class="article-footer">
          <span v-if="article.currentVersion">版本 {{ article.currentVersion }}</span>
          <span v-if="article.publishedAt">
            {{ article.currentVersion ? ' \u00B7 ' : '' }}最后更新 {{ formatDate(article.publishedAt) }}
          </span>
        </div>
      </article>

      <!-- Right side TOC -->
      <aside class="article-toc" v-if="tocItems.length > 1">
        <div class="toc-title">目录</div>
        <ul class="toc-list">
          <li
            v-for="item in tocItems"
            :key="item.id"
            :class="`toc-level-${item.level}`"
          >
            <a
              :href="`#${item.id}`"
              :class="{ active: item.id === activeTocId }"
              @click.prevent="scrollToHeading(item.id)"
            >
              {{ item.text }}
            </a>
          </li>
        </ul>
      </aside>
    </div>
  </div>
  <div v-else-if="loading" class="article-loading">
    <el-skeleton :rows="10" animated />
  </div>
  <div v-else class="article-not-found">
    <el-empty description="文章不存在或已被删除" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useCategoryStore } from '@/stores/category'
import { articleApi } from '@/api/article'
import { sanitizeHtml } from '@/utils/sanitize'
import type { PublicArticle, Category } from '@/types'
import '@/styles/article.scss'

interface TocItem {
  id: string
  text: string
  level: number
}

const route = useRoute()
const categoryStore = useCategoryStore()

const article = ref<PublicArticle | null>(null)
const loading = ref(false)
const contentRef = ref<HTMLElement>()
const tocItems = ref<TocItem[]>([])
const activeTocId = ref('')

const slug = computed(() => route.params.slug as string)

const sanitizedContent = computed(() => {
  if (!article.value?.contentHtml) return ''
  return sanitizeHtml(article.value.contentHtml)
})

const breadcrumbs = computed<Category[]>(() => {
  if (!article.value) return []
  return categoryStore.getCategoryPath(article.value.categoryId)
})

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function loadArticle() {
  if (!slug.value) return
  loading.value = true
  article.value = null
  tocItems.value = []
  activeTocId.value = ''

  try {
    const { data } = await articleApi.getPublic(slug.value)
    article.value = data
    await nextTick()
    generateToc()
    observeHeadings()
  } catch {
    article.value = null
  } finally {
    loading.value = false
  }
}

function generateToc() {
  if (!contentRef.value) return
  const headings = contentRef.value.querySelectorAll('.article-html h2, .article-html h3, .article-html h4')
  const items: TocItem[] = []

  headings.forEach((heading, index) => {
    const el = heading as HTMLElement
    const id = el.id || `heading-${index}`
    if (!el.id) {
      el.id = id
    }
    const level = parseInt(el.tagName.charAt(1))
    items.push({
      id,
      text: el.textContent || '',
      level,
    })
  })

  tocItems.value = items
}

// Scroll spy
let observer: IntersectionObserver | null = null

function observeHeadings() {
  if (observer) {
    observer.disconnect()
  }

  if (!contentRef.value) return

  const headings = contentRef.value.querySelectorAll('.article-html h2, .article-html h3, .article-html h4')
  if (headings.length === 0) return

  observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          activeTocId.value = entry.target.id
          break
        }
      }
    },
    {
      rootMargin: '-80px 0px -60% 0px',
      threshold: 0,
    }
  )

  headings.forEach((h) => observer!.observe(h))
}

function scrollToHeading(id: string) {
  const el = document.getElementById(id)
  if (el) {
    const top = el.getBoundingClientRect().top + window.scrollY - 72
    window.scrollTo({ top, behavior: 'smooth' })
    activeTocId.value = id
  }
}

watch(slug, () => {
  loadArticle()
})

onMounted(() => {
  loadArticle()
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
})
</script>

<style lang="scss" scoped>
.article-page {
  max-width: 1100px;
  margin: 0 auto;
}

.breadcrumb {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0;
  font-size: 13px;
  color: var(--doc-text-secondary);
  margin-bottom: 24px;

  a {
    color: var(--doc-text-secondary);
    text-decoration: none;

    &:hover {
      color: var(--doc-primary);
    }
  }

  .separator {
    margin: 0 6px;
    color: #d1d5db;
  }

  .current {
    color: var(--doc-text);
    font-weight: 500;
  }
}

.article-with-toc {
  display: flex;
  gap: 32px;
}

.article-content {
  flex: 1;
  min-width: 0;
  max-width: var(--doc-content-max-width);

  > h1 {
    font-size: 2em;
    font-weight: 700;
    margin-bottom: 16px;
    line-height: 1.3;
  }
}

.article-footer {
  margin-top: 48px;
  padding-top: 16px;
  border-top: 1px solid var(--doc-border);
  font-size: 13px;
  color: var(--doc-text-secondary);
}

.article-toc {
  width: var(--doc-toc-width);
  flex-shrink: 0;
  position: sticky;
  top: calc(var(--doc-header-height) + 24px);
  align-self: flex-start;
  max-height: calc(100vh - var(--doc-header-height) - 48px);
  overflow-y: auto;

  @media (max-width: 1100px) {
    display: none;
  }
}

.toc-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--doc-text);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.toc-list {
  list-style: none;
  padding: 0;
  margin: 0;
  border-left: 2px solid var(--doc-border);

  li {
    &.toc-level-2 {
      padding-left: 12px;
    }
    &.toc-level-3 {
      padding-left: 24px;
    }
    &.toc-level-4 {
      padding-left: 36px;
    }
  }

  a {
    display: block;
    padding: 3px 0;
    font-size: 12px;
    line-height: 1.5;
    color: var(--doc-text-secondary);
    text-decoration: none;
    transition: color 0.15s;

    &:hover {
      color: var(--doc-primary);
      text-decoration: none;
    }

    &.active {
      color: var(--doc-primary);
      font-weight: 500;
    }
  }
}

.article-loading {
  max-width: var(--doc-content-max-width);
  margin: 0 auto;
  padding: 24px 0;
}

.article-not-found {
  padding: 80px 0;
  text-align: center;
}
</style>
