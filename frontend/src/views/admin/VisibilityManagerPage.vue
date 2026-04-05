<template>
  <div class="visibility-manager-page">
    <div class="page-header">
      <h2>可见性管理</h2>
      <div class="header-actions">
        <el-button :disabled="selectedItems.length === 0" @click="batchToggle(true)">批量显示</el-button>
        <el-button :disabled="selectedItems.length === 0" @click="batchToggle(false)">批量隐藏</el-button>
      </div>
    </div>

    <el-card v-loading="loading">
      <el-tree
        ref="treeRef"
        :data="treeData"
        node-key="nodeKey"
        default-expand-all
        show-checkbox
        @check="handleCheck"
      >
        <template #default="{ data }">
          <div class="tree-node" :class="{ 'is-hidden': !data.visible }">
            <span class="node-label">
              <el-tag v-if="data.nodeType === 'category'" size="small" type="info">分类</el-tag>
              <el-tag v-else size="small">文章</el-tag>
              {{ data.label }}
            </span>
            <el-switch
              :model-value="data.visible"
              size="small"
              @change="(val: boolean) => handleToggle(data, val)"
              @click.stop
            />
          </div>
        </template>
      </el-tree>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { visibilityApi } from '@/api/visibility'

interface VisTreeNode {
  nodeKey: string
  nodeType: 'category' | 'article'
  id: number
  label: string
  visible: boolean
  children?: VisTreeNode[]
}

const loading = ref(false)
const treeData = ref<VisTreeNode[]>([])
const selectedItems = ref<VisTreeNode[]>([])

interface RawCategory {
  id: number
  name: string
  visible: boolean
  children?: RawCategory[]
  articles?: Array<{ id: number; title: string; visible: boolean; status: string }>
}

function buildTreeData(cats: RawCategory[]): VisTreeNode[] {
  return cats.map((cat) => {
    const node: VisTreeNode = {
      nodeKey: `cat-${cat.id}`,
      nodeType: 'category',
      id: cat.id,
      label: cat.name,
      visible: cat.visible,
      children: [],
    }
    // Add articles as children
    if (cat.articles) {
      for (const art of cat.articles) {
        node.children!.push({
          nodeKey: `art-${art.id}`,
          nodeType: 'article',
          id: art.id,
          label: art.title,
          visible: art.visible,
        })
      }
    }
    // Add child categories
    if (cat.children?.length) {
      node.children!.push(...buildTreeData(cat.children))
    }
    return node
  })
}

async function loadTree() {
  loading.value = true
  try {
    const { data } = await visibilityApi.tree()
    treeData.value = buildTreeData(data as RawCategory[])
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleToggle(node: VisTreeNode, visible: boolean) {
  try {
    if (node.nodeType === 'category') {
      await visibilityApi.toggleCategory(node.id, { visible })
    } else {
      await visibilityApi.toggleArticle(node.id, { visible })
    }
    node.visible = visible
    ElMessage.success(visible ? '已设为可见' : '已设为隐藏')
  } catch {
    // Error handled by interceptor
  }
}

function handleCheck(_data: VisTreeNode, checkState: { checkedNodes: VisTreeNode[] }) {
  selectedItems.value = checkState.checkedNodes
}

async function batchToggle(visible: boolean) {
  const categoryIds = selectedItems.value.filter(n => n.nodeType === 'category').map(n => n.id)
  const articleIds = selectedItems.value.filter(n => n.nodeType === 'article').map(n => n.id)

  try {
    const promises: Promise<unknown>[] = []
    if (categoryIds.length > 0) {
      promises.push(visibilityApi.batch({ type: 'category', ids: categoryIds, visible }))
    }
    if (articleIds.length > 0) {
      promises.push(visibilityApi.batch({ type: 'article', ids: articleIds, visible }))
    }
    await Promise.all(promises)
    ElMessage.success(visible ? '批量设为可见' : '批量设为隐藏')
    loadTree()
  } catch {
    // Error handled by interceptor
  }
}

onMounted(() => {
  loadTree()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  padding-right: 16px;
}

.tree-node.is-hidden {
  opacity: 0.5;
}

.node-label {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
