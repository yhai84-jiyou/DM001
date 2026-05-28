<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'

const route = useRoute()
const router = useRouter()
const issue = ref<any>(null)
const loading = ref(false)
const newComment = ref('')

async function loadIssue() {
  loading.value = true
  try {
    const res: any = await request.get(`/issues/${route.params.id}`)
    issue.value = res.data
  } finally {
    loading.value = false
  }
}

async function doTransition(t: any) {
  await ElMessageBox.confirm(`确定将状态更新为「${t.name}」？`, '状态流转')
  try {
    await request.post(`/issues/${route.params.id}/transition`, { target_status_code: t.code })
    ElMessage.success(`状态已更新为「${t.name}」`)
    loadIssue()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.detail || '操作失败')
  }
}

async function addComment() {
  if (!newComment.value.trim()) return
  await request.post(`/issues/${route.params.id}/comments`, { content: newComment.value })
  newComment.value = ''
  ElMessage.success('评论已添加')
  loadIssue()
}

function fieldLabel(field: string): string {
  const map: Record<string, string> = {
    title: '标题', description: '描述', platform_id: '平台', issue_type_id: '问题类型',
    priority_id: '优先级', status_id: '状态', product_owner_id: '产品跟进人',
    dev_owner_id: '研发跟进人', test_owner_id: '测试跟进人', expected_date: '期望解决日期',
  }
  return map[field] || field
}

onMounted(loadIssue)
</script>

<template>
  <div v-loading="loading" v-if="issue">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <div>
        <el-button text @click="router.push('/issues')">← 返回列表</el-button>
        <span style="font-size: 14px; color: #999; margin-right: 12px">{{ issue.issue_no }}</span>
        <span style="font-size: 20px; font-weight: bold">{{ issue.title }}</span>
      </div>
      <el-button type="primary" size="small" @click="router.push(`/issues/${issue.id}/edit`)">编辑</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card style="margin-bottom: 16px">
          <template #header><span style="font-weight: bold">基本信息</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="平台">
              <el-tag v-if="issue.platform" :color="issue.platform.color" style="color:#fff" size="small">{{ issue.platform.name }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="问题类型">{{ issue.issue_type?.name }}</el-descriptions-item>
            <el-descriptions-item label="优先级">
              <el-tag v-if="issue.priority" :color="issue.priority.color" style="color:#fff" size="small">{{ issue.priority.name }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag v-if="issue.status" :color="issue.status.color" style="color:#fff">{{ issue.status.name }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="提交人">{{ issue.submitter?.name }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ issue.submitted_at?.replace('T',' ').slice(0,19) }}</el-descriptions-item>
            <el-descriptions-item label="产品跟进">{{ issue.product_owner?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="研发跟进">{{ issue.dev_owner?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="测试跟进">{{ issue.test_owner?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="期望解决">{{ issue.expected_date || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card v-if="issue.description" style="margin-bottom: 16px">
          <template #header><span style="font-weight: bold">详细描述</span></template>
          <div style="white-space: pre-wrap; line-height: 1.6">{{ issue.description }}</div>
        </el-card>

        <el-card style="margin-bottom: 16px">
          <template #header><span style="font-weight: bold">评论 ({{ issue.comments?.length || 0 }})</span></template>
          <div v-for="c in issue.comments" :key="c.id" style="border-bottom: 1px solid #eee; padding: 12px 0">
            <div style="display: flex; justify-content: space-between">
              <span style="font-weight: bold">{{ c.author?.name }}</span>
              <span style="color: #999; font-size: 12px">{{ c.created_at?.replace('T',' ').slice(0,19) }}</span>
            </div>
            <div style="margin-top: 4px; white-space: pre-wrap">{{ c.content }}</div>
          </div>
          <div style="margin-top: 12px; display: flex; gap: 8px">
            <el-input v-model="newComment" placeholder="添加评论..." @keyup.enter="addComment" />
            <el-button type="primary" @click="addComment">发送</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card v-if="issue.available_transitions?.length" style="margin-bottom: 16px">
          <template #header><span style="font-weight: bold">状态操作</span></template>
          <div style="display: flex; flex-wrap: wrap; gap: 8px">
            <el-button
              v-for="t in issue.available_transitions" :key="t.code"
              :style="{ background: t.color, borderColor: t.color, color: '#fff' }"
              @click="doTransition(t)"
            >
              → {{ t.name }}
            </el-button>
          </div>
        </el-card>

        <el-card>
          <template #header><span style="font-weight: bold">变更记录</span></template>
          <el-timeline>
            <el-timeline-item
              v-for="log in issue.changelog" :key="log.id"
              :timestamp="log.changed_at?.replace('T',' ').slice(0,19)"
              placement="top"
            >
              <p style="font-size: 13px">
                <strong>{{ log.changed_by?.name }}</strong>
                修改了 <em>{{ fieldLabel(log.field_name) }}</em>
              </p>
              <p v-if="log.old_value || log.new_value" style="font-size: 12px; color: #666">
                <span v-if="log.old_value" style="text-decoration: line-through; color: #f56c6c">{{ log.old_value }}</span>
                <span v-if="log.old_value && log.new_value"> → </span>
                <span v-if="log.new_value" style="color: #67c23a">{{ log.new_value }}</span>
              </p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="!issue.changelog?.length" description="暂无变更记录" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
