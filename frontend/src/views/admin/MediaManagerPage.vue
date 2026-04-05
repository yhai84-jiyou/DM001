<template>
  <div class="media-manager-page">
    <div class="page-header">
      <h2>媒体管理</h2>
      <div class="header-actions">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="grid">网格</el-radio-button>
          <el-radio-button value="list">列表</el-radio-button>
        </el-radio-group>
        <el-upload
          :show-file-list="false"
          :before-upload="handleUpload"
          accept="image/*,video/*,application/pdf"
          multiple
        >
          <el-button type="primary" :icon="Upload">上传文件</el-button>
        </el-upload>
      </div>
    </div>

    <!-- Upload progress -->
    <el-card v-if="uploadProgress > 0 && uploadProgress < 100" style="margin-bottom: 16px;">
      <el-progress :percentage="uploadProgress" :stroke-width="8" />
    </el-card>

    <el-card v-loading="loading">
      <!-- Grid View -->
      <div v-if="viewMode === 'grid'" class="media-grid">
        <div
          v-for="item in mediaList"
          :key="item.id"
          class="media-card"
        >
          <div class="media-thumbnail">
            <el-image
              v-if="isImage(item.contentType)"
              :src="getFileUrl(item)"
              :preview-src-list="[getFileUrl(item)]"
              fit="cover"
              style="width: 100%; height: 140px;"
            />
            <div v-else class="file-icon">
              <el-icon :size="48" color="#909399">
                <VideoPlay v-if="isVideo(item.contentType)" />
                <Document v-else />
              </el-icon>
            </div>
          </div>
          <div class="media-info">
            <div class="media-name" :title="item.originalName">{{ item.originalName }}</div>
            <div class="media-meta">{{ formatSize(item.fileSize) }}</div>
          </div>
          <div class="media-actions">
            <el-button link type="primary" size="small" @click="copyUrl(item)">复制链接</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(item)">删除</el-button>
          </div>
        </div>
      </div>

      <!-- List View -->
      <el-table v-else :data="mediaList" stripe style="width: 100%">
        <el-table-column label="预览" width="80">
          <template #default="{ row }">
            <el-image
              v-if="isImage(row.contentType)"
              :src="getFileUrl(row)"
              :preview-src-list="[getFileUrl(row)]"
              fit="cover"
              style="width: 48px; height: 48px; border-radius: 4px;"
            />
            <el-icon v-else :size="32" color="#909399">
              <VideoPlay v-if="isVideo(row.contentType)" />
              <Document v-else />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            {{ row.contentType }}
          </template>
        </el-table-column>
        <el-table-column label="大小" width="100">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="copyUrl(row)">复制链接</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[12, 24, 48]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadMedia"
          @current-change="loadMedia"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Upload, VideoPlay, Document } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRawFile } from 'element-plus'
import { mediaApi } from '@/api/media'
import type { Media } from '@/types'

const viewMode = ref<'grid' | 'list'>('grid')
const mediaList = ref<Media[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const uploadProgress = ref(0)

function isImage(contentType: string): boolean {
  return contentType.startsWith('image/')
}

function isVideo(contentType: string): boolean {
  return contentType.startsWith('video/')
}

function getFileUrl(item: Media): string {
  return `/uploads/${item.storedName}`
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadMedia() {
  loading.value = true
  try {
    const { data } = await mediaApi.list({
      page: currentPage.value - 1,
      size: pageSize.value,
    })
    mediaList.value = data.content
    total.value = data.totalElements
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleUpload(file: UploadRawFile): boolean {
  const formData = new FormData()
  formData.append('file', file)
  uploadProgress.value = 1

  mediaApi.upload(formData, (percent) => {
    uploadProgress.value = percent
  }).then(() => {
    ElMessage.success('上传成功')
    uploadProgress.value = 0
    loadMedia()
  }).catch(() => {
    uploadProgress.value = 0
  })

  return false
}

function copyUrl(item: Media) {
  const url = window.location.origin + getFileUrl(item)
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

async function handleDelete(item: Media) {
  try {
    await ElMessageBox.confirm(`确定删除文件「${item.originalName}」？`, '提示', { type: 'warning' })
    await mediaApi.delete(item.id)
    ElMessage.success('删除成功')
    loadMedia()
  } catch {
    // cancelled or error
  }
}

onMounted(() => {
  loadMedia()
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
  gap: 12px;
  align-items: center;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.media-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.media-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.media-thumbnail {
  height: 140px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.media-info {
  padding: 8px 12px;
}

.media-name {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.media-actions {
  padding: 4px 12px 8px;
  display: flex;
  gap: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
