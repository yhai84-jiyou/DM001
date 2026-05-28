<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'

const categories = ref<any[]>([])
const selectedCat = ref<any>(null)
const items = ref<any[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const editingItem = ref<any>(null)
const form = ref({ name: '', code: '', color: '#409EFF', sort_order: 0, is_default: false, extra_config: '' })

async function loadCategories() {
  const res: any = await request.get('/dictionary/categories')
  categories.value = res.data
  if (categories.value.length > 0 && !selectedCat.value) {
    selectCategory(categories.value[0])
  }
}

async function selectCategory(cat: any) {
  selectedCat.value = cat
  loading.value = true
  try {
    const res: any = await request.get(`/dictionary/${cat.code}/items`, { params: { include_inactive: true } })
    items.value = res.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingItem.value = null
  form.value = { name: '', code: '', color: '#409EFF', sort_order: 0, is_default: false, extra_config: '' }
  dialogVisible.value = true
}

function openEdit(item: any) {
  editingItem.value = item
  form.value = {
    name: item.name, code: item.code, color: item.color || '#409EFF',
    sort_order: item.sort_order, is_default: item.is_default,
    extra_config: item.extra_config ? JSON.stringify(item.extra_config) : '',
  }
  dialogVisible.value = true
}

async function saveItem() {
  const data: any = { ...form.value }
  if (data.extra_config) {
    try { data.extra_config = JSON.parse(data.extra_config) } catch { ElMessage.error('扩展配置JSON格式错误'); return }
  } else {
    data.extra_config = null
  }

  if (editingItem.value) {
    await request.put(`/dictionary/items/${editingItem.value.id}`, data)
    ElMessage.success('更新成功')
  } else {
    await request.post(`/dictionary/${selectedCat.value.code}/items`, data)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  selectCategory(selectedCat.value)
}

async function deleteItem(item: any) {
  await ElMessageBox.confirm(`确定删除「${item.name}」？`, '确认')
  try {
    await request.delete(`/dictionary/items/${item.id}`)
    ElMessage.success('已删除')
    selectCategory(selectedCat.value)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.detail || '删除失败')
  }
}

async function toggleActive(item: any) {
  await request.put(`/dictionary/items/${item.id}`, { is_active: !item.is_active })
  ElMessage.success(item.is_active ? '已禁用' : '已启用')
  selectCategory(selectedCat.value)
}

onMounted(loadCategories)
</script>

<template>
  <div style="display: flex; gap: 16px; height: calc(100vh - 160px)">
    <el-card style="width: 220px; flex-shrink: 0">
      <template #header><span style="font-weight: bold">字典分类</span></template>
      <div
        v-for="cat in categories" :key="cat.id"
        :style="{ padding: '8px 12px', cursor: 'pointer', borderRadius: '4px', background: selectedCat?.id === cat.id ? '#ecf5ff' : '', marginBottom: '4px' }"
        @click="selectCategory(cat)"
      >
        {{ cat.name }}
        <el-tag v-if="cat.is_system" size="small" type="info" style="margin-left: 4px">内置</el-tag>
      </div>
    </el-card>

    <el-card style="flex: 1">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: bold">{{ selectedCat?.name || '请选择分类' }}</span>
          <el-button v-if="selectedCat" type="primary" size="small" @click="openCreate">新增字典项</el-button>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" width="160" />
        <el-table-column prop="code" label="编码" width="180" />
        <el-table-column label="颜色" width="80">
          <template #default="{ row }">
            <div v-if="row.color" :style="{ width: '24px', height: '24px', borderRadius: '4px', background: row.color }" />
          </template>
        </el-table-column>
        <el-table-column prop="sort_order" label="排序" width="70" />
        <el-table-column label="默认" width="70">
          <template #default="{ row }"><el-tag v-if="row.is_default" size="small" type="success">是</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.is_active ? 'success' : 'danger'" size="small">{{ row.is_active ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" @click="toggleActive(row)">{{ row.is_active ? '禁用' : '启用' }}</el-button>
            <el-button v-if="!row.is_system" size="small" type="danger" @click="deleteItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingItem ? '编辑字典项' : '新增字典项'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" :disabled="!!editingItem" /></el-form-item>
        <el-form-item label="颜色"><el-color-picker v-model="form.color" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort_order" :min="0" /></el-form-item>
        <el-form-item label="默认"><el-switch v-model="form.is_default" /></el-form-item>
        <el-form-item label="扩展配置"><el-input v-model="form.extra_config" type="textarea" :rows="3" placeholder="JSON格式" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
