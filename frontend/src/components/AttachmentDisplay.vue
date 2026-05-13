<template>
  <div>
    <!-- ========== 附件缩略图网格 -- File list mode ========== -->
    <template v-if="files && files.length > 0">
      <div class="attachment-grid">
        <div v-for="(file, idx) in files" :key="idx" class="attachment-item">
          <el-image
            v-if="file.fileType === 'IMAGE'"
            :src="getFileUrl(file.id)"
            fit="cover"
            class="attachment-thumb"
            :preview-src-list="imagePreviewList"
            hide-on-click-modal
            preview-teleported
          />
          <div v-else class="attachment-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="attachment-info">
            <span class="attachment-name" :title="file.originalName || `文件 ${file.id}`">{{ file.originalName || `文件 ${file.id}` }}</span>
            <div class="attachment-actions">
              <el-button
                v-if="file.fileType === 'IMAGE' || isPdfFile(file)"
                type="primary" link size="small"
                @click="openPreview('files', idx)"
              >预览</el-button>
              <el-button type="primary" link size="small" @click="downloadFile(file)">
                <el-icon><Download /></el-icon> 下载
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ========== 附件缩略图网格 -- URL list mode ========== -->
    <template v-else-if="fileUrls && fileUrls.length > 0">
      <div class="attachment-grid">
        <div v-for="(file, idx) in fileUrls" :key="idx" class="attachment-item">
          <el-image
            v-if="isImageUrl(file.url)"
            :src="fullUrl(file.url)"
            fit="cover"
            class="attachment-thumb"
            :preview-src-list="imagePreviewUrls"
            hide-on-click-modal
            preview-teleported
          />
          <div v-else class="attachment-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="attachment-info">
            <span class="attachment-name" :title="file.originalName || `文件 ${file.id}`">{{ file.originalName || `文件 ${file.id}` }}</span>
            <div class="attachment-actions">
              <el-button type="primary" link size="small" @click="openPreview('fileUrls', idx)">预览</el-button>
              <el-button type="primary" link size="small" @click="downloadUrlFile(file)">
                <el-icon><Download /></el-icon> 下载
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else description="暂无附件" :image-size="80" />

    <!-- ========== 全屏预览 ========== -->
    <FullscreenPreview
      v-model="previewVisible"
      :files="previewFiles"
      :initial-index="previewIndex"
      :context-label="contextLabel"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Document, Download } from '@element-plus/icons-vue'
import FullscreenPreview from '@/components/FullscreenPreview.vue'

const props = defineProps({
  files: { type: Array, default: null },
  fileUrls: { type: Array, default: null },
  contextLabel: { type: String, default: '' }
})

// ========== 全屏预览状态 ==========
const previewVisible = ref(false)
const previewFiles = ref([])
const previewIndex = ref(0)

function openPreview(mode, idx) {
  const source = mode === 'files' ? props.files : props.fileUrls
  // 转换格式：统一为 FullscreenPreview 所需的 {id, url, originalName, fileType}
  previewFiles.value = (source || []).map(f => {
    if (mode === 'files') {
      return {
        id: f.id,
        url: getFileUrl(f.id),
        originalName: f.originalName,
        fileType: f.fileType
      }
    }
    return {
      id: f.id,
      url: fullUrl(f.url),
      originalName: f.originalName,
      fileType: f.fileType || (isImageUrl(f.url) ? 'IMAGE' : 'OTHER')
    }
  })
  previewIndex.value = Math.max(0, Math.min(idx, previewFiles.value.length - 1))
  previewVisible.value = true
}

// ========== 工具函数 ==========
function getFileUrl(id) { return `/api/files/${id}` }
function fullUrl(url) { return url && url.startsWith('http') ? url : url }
function isImageUrl(url) { return /\.(png|jpg|jpeg|gif|bmp|webp)$/i.test(url || '') }
function isPdfFile(file) { return (file.originalName || '').toLowerCase().endsWith('.pdf') }
function getFileExtension(name) {
  const i = (name || '').lastIndexOf('.')
  return i >= 0 ? name.substring(i) : ''
}

const imagePreviewList = computed(() => {
  return (props.files || []).filter(f => f.fileType === 'IMAGE').map(f => getFileUrl(f.id))
})
const imagePreviewUrls = computed(() => {
  return (props.fileUrls || []).filter(f => isImageUrl(f.url)).map(f => fullUrl(f.url))
})

// ========== 下载 ==========
function downloadFile(file) {
  let url = `/api/files/${file.id}/download`
  if (props.contextLabel && file.originalName) {
    url += `?filename=${encodeURIComponent(props.contextLabel + getFileExtension(file.originalName))}`
  }
  triggerDownload(url)
}

function downloadUrlFile(file) {
  let url = `/api/files/${file.id}/download`
  if (props.contextLabel && file.originalName) {
    url += `?filename=${encodeURIComponent(props.contextLabel + getFileExtension(file.originalName))}`
  }
  triggerDownload(url)
}

function triggerDownload(url) {
  const link = document.createElement('a')
  link.href = url
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
</script>

<style scoped>
.attachment-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 12px;
}
.attachment-item {
  border: 1px solid var(--border-color);
  border-radius: var(--card-radius);
  padding: 12px;
  width: 200px;
  text-align: center;
  transition: box-shadow var(--transition-speed);
  background: var(--card-bg);
}
.attachment-item:hover {
  box-shadow: var(--shadow-default);
}
.attachment-thumb {
  width: 100%;
  height: 120px;
  border-radius: 8px;
  object-fit: cover;
}
.attachment-icon {
  width: 100%;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: var(--primary-color);
  background: var(--hover-bg);
  border-radius: 8px;
}
.attachment-info { margin-top: 10px; }
.attachment-name {
  display: block;
  font-size: 13px;
  color: var(--text-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.attachment-actions {
  margin-top: 8px;
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
