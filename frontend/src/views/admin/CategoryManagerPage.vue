<template>
  <div class="category-manager-page">
    <div class="page-header">
      <h2>分类管理</h2>
      <el-button type="primary" :icon="Plus" @click="handleAddRoot">添加根分类</el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>分类树</span>
          </template>
          <el-tree
            ref="treeRef"
            :data="categoryStore.adminTree"
            node-key="id"
            default-expand-all
            draggable
            :allow-drop="allowDrop"
            @node-drop="handleDrop"
            @node-click="handleNodeClick"
            highlight-current
          >
            <template #default="{ data }">
              <span class="tree-node-label">
                <span v-if="data.icon" class="node-icon">{{ data.icon }}</span>
                <span>{{ data.name }}</span>
                <span class="node-actions">
                  <el-button link size="small" type="primary" @click.stop="handleAddChild(data)">
                    <el-icon><Plus /></el-icon>
                  </el-button>
                  <el-button link size="small" type="danger" @click.stop="handleDeleteCategory(data)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </span>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>{{ editForm.id ? '编辑分类' : '新建分类' }}</span>
          </template>
          <el-form :model="editForm" label-width="80px">
            <el-form-item label="名称">
              <el-input v-model="editForm.name" placeholder="请输入分类名称" />
            </el-form-item>
            <el-form-item label="别名">
              <el-input v-model="editForm.slug" placeholder="URL 别名，如 getting-started" />
            </el-form-item>
            <el-form-item label="图标">
              <el-input v-model="editForm.icon" placeholder="emoji 图标，如 📖" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">
                {{ editForm.id ? '保存修改' : '创建' }}
              </el-button>
              <el-button v-if="editForm.id" @click="resetForm">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/category'
import { useCategoryStore } from '@/stores/category'
import type { Category } from '@/types'

interface TreeNode {
  id: number
  parentId: number | null
  children?: TreeNode[]
  sortOrder: number
}

const categoryStore = useCategoryStore()
const saving = ref(false)

const editForm = ref({
  id: null as number | null,
  parentId: null as number | null,
  name: '',
  slug: '',
  icon: '',
})

function resetForm() {
  editForm.value = { id: null, parentId: null, name: '', slug: '', icon: '' }
}

function handleNodeClick(data: Category) {
  editForm.value = {
    id: data.id,
    parentId: data.parentId,
    name: data.name,
    slug: data.slug,
    icon: data.icon || '',
  }
}

function handleAddRoot() {
  resetForm()
}

function handleAddChild(data: Category) {
  resetForm()
  editForm.value.parentId = data.id
}

async function handleSave() {
  if (!editForm.value.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  if (!editForm.value.slug) {
    ElMessage.warning('请输入别名')
    return
  }
  saving.value = true
  try {
    if (editForm.value.id) {
      await categoryApi.update(editForm.value.id, {
        name: editForm.value.name,
        slug: editForm.value.slug,
        icon: editForm.value.icon || undefined,
      })
      ElMessage.success('更新成功')
    } else {
      await categoryApi.create({
        name: editForm.value.name,
        slug: editForm.value.slug,
        parentId: editForm.value.parentId || undefined,
        icon: editForm.value.icon || undefined,
      })
      ElMessage.success('创建成功')
    }
    resetForm()
    categoryStore.fetchAdminTree()
  } catch {
    // Error handled by interceptor
  } finally {
    saving.value = false
  }
}

async function handleDeleteCategory(data: Category) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${data.name}」？`, '提示', { type: 'warning' })
    await categoryApi.delete(data.id)
    ElMessage.success('删除成功')
    resetForm()
    categoryStore.fetchAdminTree()
  } catch {
    // cancelled or error
  }
}

function allowDrop(_draggingNode: unknown, _dropNode: unknown, type: string) {
  // Allow drop as sibling or child
  return type !== 'none'
}

function flattenTree(nodes: TreeNode[], parentId: number | null): Array<{ id: number; parentId: number | null; sortOrder: number }> {
  const result: Array<{ id: number; parentId: number | null; sortOrder: number }> = []
  nodes.forEach((node, index) => {
    result.push({ id: node.id, parentId, sortOrder: index })
    if (node.children?.length) {
      result.push(...flattenTree(node.children, node.id))
    }
  })
  return result
}

async function handleDrop() {
  // After drag, send the new order to the server
  const tree = categoryStore.adminTree as TreeNode[]
  const reorderData = flattenTree(tree, null)
  try {
    await categoryApi.reorder(reorderData)
    ElMessage.success('排序已更新')
  } catch {
    // revert by reloading
    categoryStore.fetchAdminTree()
  }
}

onMounted(() => {
  categoryStore.fetchAdminTree()
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

.tree-node-label {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  font-size: 14px;
}

.node-icon {
  font-size: 16px;
}

.node-actions {
  margin-left: auto;
  opacity: 0;
  transition: opacity 0.15s;
}

.tree-node-label:hover .node-actions {
  opacity: 1;
}
</style>
