<template>
  <li class="sidebar-node">
    <div
      class="node-label"
      :class="{ 'has-children': hasChildren }"
      @click="toggle"
    >
      <span class="node-arrow" v-if="hasChildren" :class="{ expanded }">
        <svg width="12" height="12" viewBox="0 0 12 12">
          <path d="M4 2 L8 6 L4 10" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
        </svg>
      </span>
      <span class="node-icon" v-if="category.icon">{{ category.icon }}</span>
      <span class="node-text">{{ category.name }}</span>
    </div>

    <ul class="node-children" v-if="expanded">
      <!-- Articles in this category -->
      <li
        v-for="art in articles"
        :key="'a-' + art.id"
        class="article-link"
        :class="{ active: art.slug === currentSlug }"
      >
        <router-link :to="`/doc/${art.slug}`" @click.native="$emit('select', art)">
          {{ art.title }}
        </router-link>
      </li>

      <!-- Child categories (recursive) -->
      <SidebarNode
        v-for="child in category.children"
        :key="child.id"
        :category="child"
        :current-slug="currentSlug"
        @select="$emit('select', $event)"
      />
    </ul>
  </li>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { Category, PublicArticle } from '@/types'
import { articleApi } from '@/api/article'

const props = defineProps<{
  category: Category
  currentSlug?: string
}>()

defineEmits<{
  select: [article: PublicArticle]
}>()

const expanded = ref(false)
const articles = ref<PublicArticle[]>([])
const loaded = ref(false)

const hasChildren = computed(() => {
  return (props.category.children && props.category.children.length > 0) || true
})

async function loadArticles() {
  if (loaded.value) return
  try {
    const { data } = await articleApi.listByCategory(props.category.id)
    articles.value = data
  } catch {
    // Category may have no articles
  }
  loaded.value = true
}

function toggle() {
  expanded.value = !expanded.value
  if (expanded.value && !loaded.value) {
    loadArticles()
  }
}

// Auto-expand if current slug is in this category's articles
watch(
  () => props.currentSlug,
  async (slug) => {
    if (!slug) return
    if (!loaded.value) {
      await loadArticles()
    }
    if (articles.value.some(a => a.slug === slug)) {
      expanded.value = true
    }
  },
  { immediate: true }
)
</script>

<style lang="scss" scoped>
.sidebar-node {
  list-style: none;
}

.node-label {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: var(--doc-text);
  border-radius: 4px;
  user-select: none;

  &:hover {
    background: #f3f4f6;
  }
}

.node-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  transition: transform 0.15s ease;
  color: #9ca3af;
  flex-shrink: 0;

  &.expanded {
    transform: rotate(90deg);
  }
}

.node-icon {
  flex-shrink: 0;
}

.node-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-children {
  list-style: none;
  padding-left: 16px;
}

.article-link {
  list-style: none;

  a {
    display: block;
    padding: 4px 12px 4px 20px;
    font-size: 13px;
    color: var(--doc-text-secondary);
    border-radius: 4px;
    text-decoration: none;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    line-height: 1.8;

    &:hover {
      color: var(--doc-primary);
      background: #f3f4f6;
      text-decoration: none;
    }
  }

  &.active a {
    color: var(--doc-primary);
    background: var(--doc-primary-light);
    font-weight: 500;
  }
}
</style>
