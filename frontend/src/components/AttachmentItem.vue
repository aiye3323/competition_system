<template>
  <div
    class="attachment-item"
    :class="{ 'attachment-item--downloaded': downloaded }"
    :title="tooltipText"
  >
    <!-- 已下载标记 -->
    <div v-if="downloaded" class="downloaded-badge">
      <el-icon :size="14"><Check /></el-icon>
    </div>

    <!-- 复选框 -->
    <el-checkbox
      v-if="selectable"
      :model-value="selected"
      class="attach-check"
      @change="emit('toggle')"
    />

    <!-- 缩略图 / 文件图标 -->
    <el-image
      v-if="isImage"
      :src="fileUrl"
      fit="cover"
      class="attachment-thumb"
      :preview-src-list="isImage ? [fileUrl] : []"
      hide-on-click-modal
      preview-teleported
    />
    <div v-else class="attachment-icon" :class="iconClass">
      <el-icon :size="36"><component :is="iconComponent" /></el-icon>
    </div>

    <!-- 文件信息 -->
    <div class="attachment-info">
      <span class="attachment-name" :title="originalName">
        {{ displayNormativeName }}
      </span>
      <span class="attachment-original" :title="originalName">
        {{ originalName }}
      </span>
    </div>

    <!-- 操作按钮 -->
    <div class="attachment-actions">
      <el-button
        v-if="canPreviewFile"
        type="primary" link size="small"
        @click="emit('preview')"
      >预览</el-button>
      <el-button type="primary" link size="small" @click="handleDownload">
        <el-icon><Download /></el-icon> 下载
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Document, Download, Check, VideoCamera } from '@element-plus/icons-vue'
import { generateFileName } from '@/utils/fileNaming'
import { downloadSingleFile } from '@/api/file'
import { downloadBlob } from '@/utils/fileNaming'

const props = defineProps({
  /** 文件对象（含 id, originalName, fileType 等） */
  file: { type: Object, required: true },
  /** 文件 URL（可选，不传则用 /api/files/{id}） */
  fileUrl: { type: String, default: '' },
  /** 命名参数 — 传给 generateFileName */
  namingParams: { type: Object, default: () => ({}) },
  /** 是否显示复选框 */
  selectable: { type: Boolean, default: false },
  /** 是否选中 */
  selected: { type: Boolean, default: false }
})

const emit = defineEmits(['preview', 'toggle', 'downloaded'])

const downloaded = ref(false)

const originalName = computed(() => props.file.originalName || `文件 ${props.file.id}`)
const resolvedUrl = computed(() => props.fileUrl || `/api/files/${props.file.id}`)

const displayNormativeName = computed(() => generateFileName({
  ...props.namingParams,
  originalName: originalName.value
}))

const tooltipText = computed(() =>
  `规范名：${displayNormativeName.value}\n原始名：${originalName.value}` +
  (downloaded.value ? '\n状态：已下载' : '')
)

const isImage = computed(() => props.file.fileType === 'IMAGE'
  || /\.(png|jpg|jpeg|gif|bmp|webp)$/i.test(originalName.value))

const isVideo = computed(() => props.file.fileType === 'VIDEO'
  || /\.(mp4|avi|mov|webm)$/i.test(originalName.value))

const canPreviewFile = computed(() => {
  const name = originalName.value.toLowerCase()
  return props.file.fileType === 'IMAGE'
    || isVideo.value
    || name.endsWith('.pdf')
    || /\.(docx?|xlsx?|pptx?|txt)$/i.test(name)
})

const iconComponent = computed(() => isVideo.value ? VideoCamera : Document)
const iconClass = computed(() => isVideo.value ? 'icon-video' : 'icon-doc')

async function handleDownload() {
  try {
    const blob = await downloadSingleFile(props.file.id)
    const name = displayNormativeName.value
    downloadBlob(blob, name)
    downloaded.value = true
    emit('downloaded', props.file)
  } catch {
    // Vue 全局错误处理
  }
}
</script>

<style scoped>
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

.attachment-item--downloaded {
  border-color: var(--success-color);
}

.attachment-item--downloaded::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: var(--card-radius);
  border: 2px solid var(--success-color);
  pointer-events: none;
  opacity: 0.3;
}

.downloaded-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--success-color);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
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

.attachment-info {
  margin-top: 10px;
}

.attachment-name {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-original {
  display: block;
  font-size: 11px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.attachment-actions {
  margin-top: 8px;
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
