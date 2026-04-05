import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Category } from '@/types'
import { categoryApi } from '@/api/category'

export const useCategoryStore = defineStore('category', () => {
  const publicTree = ref<Category[]>([])
  const adminTree = ref<Category[]>([])
  const loading = ref(false)

  async function fetchPublicTree() {
    loading.value = true
    try {
      const { data } = await categoryApi.publicTree()
      publicTree.value = data
    } finally {
      loading.value = false
    }
  }

  async function fetchAdminTree() {
    loading.value = true
    try {
      const { data } = await categoryApi.tree()
      adminTree.value = data
    } finally {
      loading.value = false
    }
  }

  // Flatten tree for breadcrumb lookups
  function findCategory(id: number, tree?: Category[]): Category | null {
    const nodes = tree || publicTree.value
    for (const node of nodes) {
      if (node.id === id) return node
      if (node.children) {
        const found = findCategory(id, node.children)
        if (found) return found
      }
    }
    return null
  }

  // Build breadcrumb path
  function getCategoryPath(id: number): Category[] {
    const path: Category[] = []
    let current = findCategory(id)
    while (current) {
      path.unshift(current)
      current = current.parentId ? findCategory(current.parentId) : null
    }
    return path
  }

  return { publicTree, adminTree, loading, fetchPublicTree, fetchAdminTree, findCategory, getCategoryPath }
})
