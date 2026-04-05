<template>
  <div class="article-editor-page">
    <div class="editor-header">
      <div class="header-left">
        <el-button @click="$router.push('/admin/articles')">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h3 style="margin: 0 16px;">{{ isEdit ? '编辑文章' : '新建文章' }}</h3>
      </div>
      <div class="header-actions">
        <el-button v-if="isEdit" @click="showVersionDrawer = true">版本历史</el-button>
        <el-button :loading="saving" @click="handleSave">保存</el-button>
        <el-button type="primary" @click="handlePublishClick">发布</el-button>
      </div>
    </div>

    <div class="editor-body" v-loading="pageLoading">
      <div class="editor-meta">
        <el-form :inline="true" label-width="80px">
          <el-form-item label="标题" style="flex: 1;">
            <el-input v-model="form.title" placeholder="请输入文章标题" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="分类">
            <el-tree-select
              v-model="form.categoryId"
              :data="categoryTreeData"
              :props="{ label: 'name', value: 'id', children: 'children' }"
              placeholder="选择分类"
              check-strictly
              style="width: 240px;"
            />
          </el-form-item>
          <el-form-item v-if="!isEdit" label="编辑器">
            <el-radio-group v-model="form.editorMode">
              <el-radio value="RICH">富文本</el-radio>
              <el-radio value="MARKDOWN">Markdown</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="!isEdit">
            <el-upload
              :show-file-list="false"
              :before-upload="handleImport"
              accept=".md,.docx"
            >
              <el-button>
                <el-icon><Upload /></el-icon>
                导入文件
              </el-button>
            </el-upload>
          </el-form-item>
        </el-form>
      </div>

      <div class="editor-area">
        <RichTextEditor
          v-if="form.editorMode === 'RICH'"
          v-model="form.content"
          @update:html="form.contentHtml = $event"
          placeholder="请输入文章内容..."
        />
        <MarkdownEditor
          v-else
          v-model="form.content"
          placeholder="请输入 Markdown 内容..."
        />
      </div>
    </div>

    <!-- Publish Dialog -->
    <el-dialog v-model="publishDialogVisible" title="发布文章" width="480px">
      <el-form :model="publishForm" label-width="80px">
        <el-form-item label="版本号">
          <el-input v-model="publishForm.versionLabel" placeholder="例如 v1.0" />
        </el-form-item>
        <el-form-item label="更新说明">
          <el-input
            v-model="publishForm.changeNotes"
            type="textarea"
            :rows="3"
            placeholder="请填写更新说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishLoading" @click="confirmPublish">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- Version History Drawer -->
    <el-drawer v-model="showVersionDrawer" title="版本历史" size="500px">
      <div v-loading="versionsLoading">
        <div v-if="versions.length === 0" style="text-align: center; color: #909399; padding: 40px;">
          暂无版本记录
        </div>
        <div v-for="ver in versions" :key="ver.id" class="version-item">
          <div class="version-header">
            <el-tag size="small">{{ ver.versionLabel }}</el-tag>
            <span class="version-time">{{ formatTime(ver.createdAt) }}</span>
          </div>
          <div class="version-notes" v-if="ver.changeNotes">{{ ver.changeNotes }}</div>
          <div class="version-actions">
            <el-button link type="primary" @click="previewVersion(ver)">预览</el-button>
            <el-button link type="warning" @click="handleRevert(ver)">回滚</el-button>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- Version Preview Dialog -->
    <el-dialog v-model="previewDialogVisible" :title="'版本预览: ' + previewVersionLabel" width="80%" top="5vh">
      <div class="version-preview-content" v-html="previewContent"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRawFile } from 'element-plus'
import { articleApi } from '@/api/article'
import { useCategoryStore } from '@/stores/category'
import type { ArticleVersion } from '@/types'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'

const route = useRoute()
const router = useRouter()
const categoryStore = useCategoryStore()

const articleId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : null
})
const isEdit = computed(() => !!articleId.value)

const pageLoading = ref(false)
const saving = ref(false)

const form = ref({
  title: '',
  categoryId: null as number | null,
  editorMode: 'RICH' as 'RICH' | 'MARKDOWN',
  content: '',
  contentHtml: '',
})

const categoryTreeData = computed(() => categoryStore.adminTree)

// Publish
const publishDialogVisible = ref(false)
const publishLoading = ref(false)
const publishForm = ref({ versionLabel: '', changeNotes: '' })

// Versions
const showVersionDrawer = ref(false)
const versionsLoading = ref(false)
const versions = ref<ArticleVersion[]>([])

// Preview
const previewDialogVisible = ref(false)
const previewContent = ref('')
const previewVersionLabel = ref('')

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadArticle() {
  if (!articleId.value) return
  pageLoading.value = true
  try {
    const { data } = await articleApi.get(articleId.value)
    form.value.title = data.title
    form.value.categoryId = data.categoryId
    form.value.editorMode = data.editorMode
    form.value.content = data.draftContent || ''
    form.value.contentHtml = data.draftContentHtml || ''
  } catch {
    ElMessage.error('加载文章失败')
  } finally {
    pageLoading.value = false
  }
}

async function handleSave() {
  if (!form.value.title) {
    ElMessage.warning('请输入文章标题')
    return
  }
  saving.value = true
  try {
    if (isEdit.value && articleId.value) {
      await articleApi.update(articleId.value, {
        title: form.value.title,
        draftContent: form.value.content,
        draftContentHtml: form.value.contentHtml || form.value.content,
        categoryId: form.value.categoryId || undefined,
      })
      ElMessage.success('保存成功')
    } else {
      if (!form.value.categoryId) {
        ElMessage.warning('请选择分类')
        saving.value = false
        return
      }
      const { data } = await articleApi.create({
        categoryId: form.value.categoryId,
        title: form.value.title,
        editorMode: form.value.editorMode,
        draftContent: form.value.content,
      })
      ElMessage.success('创建成功')
      router.replace(`/admin/articles/${data.id}/edit`)
    }
  } catch {
    // Error handled by interceptor
  } finally {
    saving.value = false
  }
}

function handlePublishClick() {
  if (!isEdit.value) {
    ElMessage.warning('请先保存文章')
    return
  }
  publishForm.value = { versionLabel: '', changeNotes: '' }
  publishDialogVisible.value = true
}

async function confirmPublish() {
  if (!articleId.value) return
  if (!publishForm.value.versionLabel) {
    ElMessage.warning('请输入版本号')
    return
  }
  publishLoading.value = true
  try {
    await articleApi.publish(articleId.value, publishForm.value)
    ElMessage.success('发布成功')
    publishDialogVisible.value = false
    loadArticle()
  } catch {
    // Error handled by interceptor
  } finally {
    publishLoading.value = false
  }
}

async function loadVersions() {
  if (!articleId.value) return
  versionsLoading.value = true
  try {
    const { data } = await articleApi.versions(articleId.value)
    versions.value = data
  } catch {
    // ignore
  } finally {
    versionsLoading.value = false
  }
}

function previewVersion(ver: ArticleVersion) {
  previewVersionLabel.value = ver.versionLabel
  previewContent.value = ver.contentHtml || ver.content
  previewDialogVisible.value = true
}

async function handleRevert(ver: ArticleVersion) {
  if (!articleId.value) return
  try {
    await ElMessageBox.confirm(`确定回滚到版本 ${ver.versionLabel}？当前草稿内容将被覆盖。`, '提示', { type: 'warning' })
    await articleApi.revert(articleId.value, ver.id)
    ElMessage.success('回滚成功')
    showVersionDrawer.value = false
    loadArticle()
  } catch {
    // cancelled or error
  }
}

function handleImport(file: UploadRawFile): boolean {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('categoryId', String(form.value.categoryId || ''))
  formData.append('editorMode', form.value.editorMode)
  articleApi.importFile(formData).then(({ data }) => {
    form.value.content = data.draftContent || ''
    form.value.contentHtml = data.draftContentHtml || ''
    form.value.title = data.title || form.value.title
    ElMessage.success('导入成功')
  }).catch(() => {
    ElMessage.error('导入失败')
  })
  return false // prevent auto upload
}

// Watch version drawer open
import { watch } from 'vue'
watch(showVersionDrawer, (val) => {
  if (val) loadVersions()
})

onMounted(async () => {
  await categoryStore.fetchAdminTree()
  if (isEdit.value) {
    loadArticle()
  }
})
</script>

<style scoped>
.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  background: #fff;
  padding: 12px 20px;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.editor-body {
  background: #fff;
  border-radius: 4px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.editor-meta {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.editor-area {
  min-height: 400px;
}

.version-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.version-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.version-time {
  font-size: 12px;
  color: #909399;
}

.version-notes {
  font-size: 13px;
  color: #606266;
  margin: 4px 0;
}

.version-actions {
  margin-top: 4px;
}

.version-preview-content {
  max-height: 70vh;
  overflow: auto;
  padding: 16px;
}
</style>
