<template>
  <nav class="sidebar-tree">
    <ul class="tree-list">
      <SidebarNode
        v-for="cat in categories"
        :key="cat.id"
        :category="cat"
        :current-slug="currentSlug"
        @select="$emit('select', $event)"
      />
    </ul>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { Category, PublicArticle } from '@/types'
import SidebarNode from './SidebarNode.vue'

defineProps<{
  categories: Category[]
}>()

defineEmits<{
  select: [article: PublicArticle]
}>()

const route = useRoute()
const currentSlug = computed(() => route.params.slug as string | undefined)
</script>

<style lang="scss" scoped>
.sidebar-tree {
  padding: 12px 8px;
}

.tree-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
</style>
