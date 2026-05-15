<template>
  <div>
    <!-- ========== 附件缩略图网格 -- File list mode ========== -->
    <template v-if="files && files.length > 0">
      <!-- 操作栏 -->
      <div v-if="showBatchActions && files.length > 0" class="attach-toolbar">
        <el-checkbox
          :model-value="allSelected"
          :indeterminate="someSelected && !allSelected"
          @change="toggleAll"
        >全选</el-checkbox>
        <div class="attach-toolbar-actions">
          <el-button
            v-if="selectedIds.length > 0"
            type="primary"
            size="small"
            @click="downloadSelected"
          >
            <el-icon><Download /></el-icon> 下载选中 ({{ selectedIds.length }})
          </el-button>
          <el-button
            v-if="relatedType && relatedId"
            size="small"
            @click="downloadAll"
          >
            <el-icon><Folder /></el-icon> 下载全部材料
          </el-button>
        </div>
      </div>

      <div class="attachment-grid">
        <div v-for="(file, idx) in files" :key="idx" class="attachment-item">
          <!-- 复选框 -->
          <el-checkbox
            v-if="showBatchActions"
            :model-value="selectedIds.includes(file.id)"
            class="attach-check"
            @change="(val) => toggleFile(file.id, val)"
          />

          <el-image
            v-if="file.fileType === 'IMAGE'"
            :src="getFileUrl(file.id)"
            fit="cover"
            class="attachment-thumb"
            :preview-src-list="imagePreviewList"
            hide-on-click-modal
            preview-teleported
          />
          <div v-else class="attachment-icon" :class="iconClass(file)">
            <el-icon :size="36"><component :is="fileIcon(file)" /></el-icon>
          </div>
          <div class="attachment-info">
            <span class="attachment-name" :title="file.originalName || `文件 ${file.id}`">
              {{ file.originalName || `文件 ${file.id}` }}
            </span>
            <div class="attachment-actions">
              <el-button
                v-if="canPreview(file)"
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
          <el-checkbox
            v-if="showBatchActions"
            :model-value="selectedUrlIdxs.includes(idx)"
            class="attach-check"
            @change="(val) => toggleUrlIdx(idx, val)"
          />

          <el-image
            v-if="isImageUrl(file.url)"
            :src="fullUrl(file.url)"
            fit="cover"
            class="attachment-thumb"
            :preview-src-list="imagePreviewUrls"
            hide-on-click-modal
            preview-teleported
          />
          <div v-else class="attachment-icon" :class="urlIconClass(file)">
            <el-icon :size="36"><component :is="urlFileIcon(file)" /></el-icon>
          </div>
          <div class="attachment-info">
            <span class="attachment-name" :title="file.originalName || `文件 ${file.id}`">
              {{ file.originalName || `文件 ${file.id}` }}
            </span>
            <div class="attachment-actions">
              <el-button
                v-if="canPreviewUrl(file)"
                type="primary" link size="small"
                @click="openPreview('fileUrls', idx)"
              >预览</el-button>
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
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Download, Folder, VideoCamera, Picture } from '@element-plus/icons-vue'
import FullscreenPreview from '@/components/FullscreenPreview.vue'
import { downloadAllFiles, downloadSelectedFiles } from '@/api/file'
import request from '@/utils/request'

const props = defineProps({
  files: { type: Array, default: null },
  fileUrls: { type: Array, default: null },
  contextLabel: { type: String, default: '' },
  /** 成果类型，用于下载全部 */
  relatedType: { type: String, default: '' },
  /** 成果 ID，用于下载全部 */
  relatedId: { type: [Number, String], default: null },
  /** 是否显示批量操作栏 */
  showBatchActions: { type: Boolean, default: true }
})

// ========== 全屏预览 ==========
const previewVisible = ref(false)
const previewFiles = ref([])
const previewIndex = ref(0)

function openPreview(mode, idx) {
  const source = mode === 'files' ? props.files : props.fileUrls
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

// ========== 批量选择 ==========
const selectedIds = ref([])
const selectedUrlIdxs = ref([])

const allSelected = computed(() => {
  if (props.files) return selectedIds.value.length === props.files.length && props.files.length > 0
  if (props.fileUrls) return selectedUrlIdxs.value.length === props.fileUrls.length && props.fileUrls.length > 0
  return false
})

const someSelected = computed(() => {
  if (props.files) return selectedIds.value.length > 0
  if (props.fileUrls) return selectedUrlIdxs.value.length > 0
  return false
})

function toggleFile(id, val) {
  if (val) {
    selectedIds.value = [...selectedIds.value, id]
  } else {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  }
}

function toggleUrlIdx(idx, val) {
  if (val) {
    selectedUrlIdxs.value = [...selectedUrlIdxs.value, idx]
  } else {
    selectedUrlIdxs.value = selectedUrlIdxs.value.filter(i => i !== idx)
  }
}

function toggleAll(val) {
  if (val) {
    if (props.files) selectedIds.value = props.files.map(f => f.id)
    if (props.fileUrls) selectedUrlIdxs.value = props.fileUrls.map((_, i) => i)
  } else {
    selectedIds.value = []
    selectedUrlIdxs.value = []
  }
}

// 切换成果时重置选择
watch([() => props.relatedId, () => props.files, () => props.fileUrls], () => {
  selectedIds.value = []
  selectedUrlIdxs.value = []
})

// ========== 批量下载 ==========
async function downloadSelected() {
  let ids = selectedIds.value
  // URL list mode: extract IDs
  if (ids.length === 0 && selectedUrlIdxs.value.length > 0 && props.fileUrls) {
    ids = selectedUrlIdxs.value.map(i => props.fileUrls[i]?.id).filter(Boolean)
  }
  if (ids.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }

  try {
    const blob = await downloadSelectedFiles(ids)
    triggerBlobDownload(blob, 'selected_materials.zip')
    ElMessage.success('下载完成')
  } catch {
    ElMessage.error('打包下载失败')
  }
}

function downloadAll() {
  if (!props.relatedType || !props.relatedId) return
  const url = downloadAllFiles(props.relatedType, props.relatedId)
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

function triggerBlobDownload(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// ========== 工具函数 ==========
function getFileUrl(id) { return `/api/files/${id}` }
function fullUrl(url) { return url && url.startsWith('http') ? url : url }
function isImageUrl(url) { return /\.(png|jpg|jpeg|gif|bmp|webp)$/i.test(url || '') }

function getFileExtension(name) {
  const i = (name || '').lastIndexOf('.')
  return i >= 0 ? name.substring(i) : ''
}

function canPreview(file) {
  const name = (file.originalName || '').toLowerCase()
  return file.fileType === 'IMAGE'
    || name.endsWith('.pdf')
    || /\.(doc|docx|xls|xlsx|ppt|pptx|txt)$/i.test(name)
    || file.fileType === 'VIDEO'
    || /\.(mp4|avi|mov)$/i.test(name)
}

function canPreviewUrl(file) {
  const name = (file.originalName || file.url || '').toLowerCase()
  return isImageUrl(file.url)
    || name.endsWith('.pdf')
    || /\.(doc|docx|xls|xlsx|ppt|pptx|txt)$/i.test(name)
    || /\.(mp4|avi|mov)$/i.test(name)
}

function fileIcon(file) {
  if (file.fileType === 'VIDEO' || /\.(mp4|avi|mov|webm)$/i.test(file.originalName || '')) return VideoCamera
  return Document
}

function iconClass(file) {
  if (file.fileType === 'VIDEO' || /\.(mp4|avi|mov|webm)$/i.test(file.originalName || '')) return 'icon-video'
  return 'icon-doc'
}

function urlFileIcon(file) {
  const name = (file.originalName || file.url || '').toLowerCase()
  if (/\.(mp4|avi|mov|webm)$/i.test(name)) return VideoCamera
  if (/\.(png|jpg|jpeg|gif|bmp|webp)$/i.test(name)) return Picture
  return Document
}

function urlIconClass(file) {
  const name = (file.originalName || file.url || '').toLowerCase()
  if (/\.(mp4|avi|mov|webm)$/i.test(name)) return 'icon-video'
  return 'icon-doc'
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
</script>

<style scoped>
.attach-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  margin-bottom: 12px;
  background: var(--hover-bg);
  border-radius: var(--card-radius);
}

.attach-toolbar-actions {
  display: flex;
  gap: 8px;
}

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
  transition: box-shadow var(--transition-fast);
  background: var(--card-bg);
  position: relative;
}

.attachment-item:hover {
  box-shadow: var(--shadow-md);
}

.attach-check {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
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
  color: var(--primary-color);
  background: var(--primary-light);
  border-radius: 8px;
}

.attachment-icon.icon-video {
  color: var(--warning-color);
  background: var(--warning-light);
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
