<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../api/request'
import { useDictionaryStore } from '../../stores/dictionary'

const route = useRoute()
const router = useRouter()
const dict = useDictionaryStore()
const isEdit = computed(() => !!route.params.id)
const loading = ref(false)

const form = ref({
  title: '', description: '', platform_id: '', issue_type_id: '', priority_id: '',
  product_owner_id: '', dev_owner_id: '', test_owner_id: '', expected_date: '',
})

const platforms = ref<any[]>([])
const issueTypes = ref<any[]>([])
const priorities = ref<any[]>([])

const aiText = ref('')
const aiLoading = ref(false)

async function loadDicts() {
  platforms.value = await dict.loadCategory('PLATFORM')
  issueTypes.value = await dict.loadCategory('ISSUE_TYPE')
  priorities.value = await dict.loadCategory('ISSUE_PRIORITY')

  const defaultPriority = priorities.value.find((p: any) => p.is_default)
  if (defaultPriority && !form.value.priority_id) {
    form.value.priority_id = defaultPriority.id
  }
}

async function loadIssue() {
  if (!isEdit.value) return
  const res: any = await request.get(`/issues/${route.params.id}`)
  const d = res.data
  form.value = {
    title: d.title, description: d.description || '',
    platform_id: d.platform?.id || '', issue_type_id: d.issue_type?.id || '',
    priority_id: d.priority?.id || '', product_owner_id: d.product_owner?.id || '',
    dev_owner_id: d.dev_owner?.id || '', test_owner_id: d.test_owner?.id || '',
    expected_date: d.expected_date || '',
  }
}

async function handleAIParse() {
  if (!aiText.value.trim()) { ElMessage.warning('请输入问题描述'); return }
  aiLoading.value = true
  try {
    const res: any = await request.post('/ai/parse-issue', { text: aiText.value })
    const parsed = res.data
    if (parsed.title) form.value.title = parsed.title
    if (parsed.description) form.value.description = parsed.description
    if (parsed.platform_code) {
      const p = platforms.value.find((x: any) => x.code === parsed.platform_code)
      if (p) form.value.platform_id = p.id
    }
    if (parsed.issue_type_code) {
      const t = issueTypes.value.find((x: any) => x.code === parsed.issue_type_code)
      if (t) form.value.issue_type_id = t.id
    }
    if (parsed.priority_code) {
      const pr = priorities.value.find((x: any) => x.code === parsed.priority_code)
      if (pr) form.value.priority_id = pr.id
    }
    ElMessage.success('AI解析完成，请核对后提交')
  } catch {
    ElMessage.error('AI解析失败')
  } finally {
    aiLoading.value = false
  }
}

async function handleSubmit() {
  if (!form.value.title || !form.value.platform_id || !form.value.issue_type_id) {
    ElMessage.warning('请填写必填项：标题、平台、问题类型')
    return
  }
  loading.value = true
  try {
    if (isEdit.value) {
      await request.put(`/issues/${route.params.id}`, form.value)
      ElMessage.success('更新成功')
      router.push(`/issues/${route.params.id}`)
    } else {
      const res: any = await request.post('/issues', form.value)
      ElMessage.success(`问题 ${res.data.issue_no} 创建成功`)
      router.push('/issues')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.detail || '操作失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadDicts()
  await loadIssue()
})
</script>

<template>
  <div style="max-width: 800px">
    <h3>{{ isEdit ? '编辑问题' : '新建问题' }}</h3>

    <el-card style="margin-bottom: 16px">
      <template #header><span>AI 辅助填写</span></template>
      <el-input v-model="aiText" type="textarea" :rows="3" placeholder="用大白话描述你遇到的问题，AI 会帮你自动解析填入下方表单..." />
      <el-button type="primary" :loading="aiLoading" style="margin-top: 8px" @click="handleAIParse">
        AI 智能解析
      </el-button>
    </el-card>

    <el-form :model="form" label-width="100px" label-position="top">
      <el-form-item label="标题" required>
        <el-input v-model="form.title" placeholder="一句话描述问题" />
      </el-form-item>
      <el-form-item label="详细描述">
        <el-input v-model="form.description" type="textarea" :rows="5" placeholder="详细描述问题情况..." />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="平台" required>
            <el-select v-model="form.platform_id" placeholder="选择平台" style="width: 100%">
              <el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="问题类型" required>
            <el-select v-model="form.issue_type_id" placeholder="选择类型" style="width: 100%">
              <el-option v-for="t in issueTypes" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="优先级">
            <el-select v-model="form.priority_id" placeholder="选择优先级" style="width: 100%">
              <el-option v-for="p in priorities" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="期望解决日期">
        <el-date-picker v-model="form.expected_date" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit">提 交</el-button>
        <el-button @click="router.back()">取 消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
