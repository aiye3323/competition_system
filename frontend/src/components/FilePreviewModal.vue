<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="70%"
    top="3vh"
    destroy-on-close
    @close="close"
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="preview-body">
      <!-- ========== 加载中 ========== -->
      <div v-if="loading" class="preview-state">
        <el-icon class="spin" :size="40"><Loading /></el-icon>
        <p>正在加载预览…</p>
      </div>

      <!-- ========== 错误 ========== -->
      <div v-else-if="error" class="preview-state">
        <el-icon :size="48"><WarningFilled /></el-icon>
        <p>{{ error }}</p>
        <el-button type="primary" size="small" @click="loadPreview">重试</el-button>
      </div>

      <!-- ========== 图片预览 ========== -->
      <div v-else-if="isImage" class="preview-image-stage">
        <img :src="fileUrl" class="preview-image" />
      </div>

      <!-- ========== PDF 预览 ========== -->
      <iframe
        v-else-if="isPdf"
        :src="fileUrl"
        class="preview-pdf"
        frameborder="0"
      />

      <!-- ========== DOCX mammoth.js 预览 ========== -->
      <div v-else-if="isDocx" class="preview-docx" v-html="htmlContent" />

      <!-- ========== TXT 预览 ========== -->
      <pre v-else-if="isTxt" class="preview-text">{{ textContent }}</pre>

      <!-- ========== 视频预览 ========== -->
      <video
        v-else-if="isVideo"
        :src="fileUrl"
        controls
        class="preview-video"
      />

      <!-- ========== 不支持 ========== -->
      <div v-else class="preview-state">
        <el-icon :size="48"><WarningFilled /></el-icon>
        <p>此文件格式暂不支持预览</p>
        <el-button type="primary" size="small" @click="emit('download')">下载文件</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Loading, WarningFilled } from '@element-plus/icons-vue'
import mammoth from 'mammoth'
import request from '@/utils/request'

const props = defineProps({
  visible: { type: Boolean, default: false },
  /** 文件 ID */
  fileId: { type: [Number, String], default: null },
  /** 文件原始名（用于判断类型） */
  fileName: { type: String, default: '' },
  /** 直接的文件 URL（可选，不传则用 /api/files/{id}） */
  fileUrl: { type: String, default: '' },
  /** 弹窗标题 */
  title: { type: String, default: '文件预览' }
})

const emit = defineEmits(['update:visible', 'download', 'close'])

// ========== 状态 ==========
const loading = ref(false)
const error = ref('')
const htmlContent = ref('')
const textContent = ref('')

const resolvedUrl = computed(() => {
  return props.fileUrl || (props.fileId ? `/api/files/${props.fileId}` : '')
})

const lowerName = computed(() => (props.fileName || '').toLowerCase())

const isImage = computed(() => /\.(png|jpg|jpeg|gif|bmp|webp)$/i.test(props.fileName))
const isPdf = computed(() => lowerName.value.endsWith('.pdf'))
const isDocx = computed(() => lowerName.value.endsWith('.docx'))
const isTxt = computed(() => lowerName.value.endsWith('.txt'))
const isVideo = computed(() => /\.(mp4|avi|mov|webm)$/i.test(props.fileName))

// ========== 加载 ==========
watch(() => props.fileId, () => {
  if (props.visible && props.fileId) loadPreview()
})

watch(() => props.visible, (val) => {
  if (val && props.fileId) loadPreview()
})

async function loadPreview() {
  error.value = ''
  htmlContent.value = ''
  textContent.value = ''

  // 图片/PDF/视频 → 直接通过 URL 渲染，无需 fetch
  if (isImage.value || isPdf.value || isVideo.value) return

  loading.value = true

  try {
    if (isDocx.value) {
      // mammoth.js 转换 DOCX → HTML
      const arrayBuffer = await request.get(`/files/${props.fileId}`, {
        responseType: 'arraybuffer'
      })
      const result = await mammoth.convertToHtml({ arrayBuffer })
      if (result.messages?.length) {
        console.warn('[FilePreviewModal] mammoth:', result.messages.map(m => m.message))
      }
      htmlContent.value = result.value || '<p>文档内容为空</p>'
    } else if (isTxt.value) {
      // 读取纯文本
      textContent.value = await request.get(`/files/${props.fileId}`, {
        responseType: 'text'
      })
    }
  } catch (e) {
    console.error('[FilePreviewModal]', e)
    error.value = '文件加载失败，请下载后查看'
  } finally {
    loading.value = false
  }
}

// ========== 关闭 ==========
function close() {
  loading.value = false
  error.value = ''
  htmlContent.value = ''
  textContent.value = ''
  emit('update:visible', false)
  emit('close')
}
</script>

<style scoped>
.preview-body {
  min-height: 400px;
  max-height: 72vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* 加载/错误/不支持 通用状态 */
.preview-state {
  text-align: center;
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.preview-state p {
  font-size: 15px;
  margin: 0;
}

.spin {
  animation: spin 1s linear infinite;
  color: var(--primary-color);
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

/* 图片 */
.preview-image-stage {
  width: 100%;
  display: flex;
  justify-content: center;
}

.preview-image {
  max-width: 100%;
  max-height: 68vh;
  object-fit: contain;
  border-radius: 6px;
}

/* PDF */
.preview-pdf {
  width: 100%;
  height: 68vh;
  border: none;
  background: #525659;
  border-radius: 6px;
}

/* DOCX */
.preview-docx {
  width: 100%;
  max-height: 68vh;
  overflow-y: auto;
  padding: 28px 36px;
  background: #fdfdfd;
  color: #1a1a1a;
  font-size: 15px;
  line-height: 1.8;
  border-radius: 6px;
}

.preview-docx :deep(h1) { font-size: 24px; margin: 16px 0 10px; }
.preview-docx :deep(h2) { font-size: 20px; margin: 14px 0 8px; }
.preview-docx :deep(h3) { font-size: 17px; margin: 12px 0 6px; }
.preview-docx :deep(table) { border-collapse: collapse; width: 100%; margin: 12px 0; }
.preview-docx :deep(td), .preview-docx :deep(th) { border: 1px solid #ddd; padding: 6px 12px; }
.preview-docx :deep(img) { max-width: 100%; height: auto; }

/* 纯文本 */
.preview-text {
  width: 100%;
  max-height: 68vh;
  overflow: auto;
  padding: 20px 28px;
  background: #f5f5f5;
  color: #333;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 6px;
  margin: 0;
}

/* 视频 */
.preview-video {
  max-width: 100%;
  max-height: 68vh;
  outline: none;
  border-radius: 6px;
}
</style>
