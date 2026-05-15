<template>
  <div class="docx-viewer">
    <!-- 加载中 -->
    <div v-if="loading" class="docx-state">
      <el-icon class="docx-spin" :size="40"><Loading /></el-icon>
      <p>正在加载文档…</p>
    </div>

    <!-- 错误 -->
    <div v-else-if="error" class="docx-state">
      <el-icon :size="48"><WarningFilled /></el-icon>
      <p>{{ error }}</p>
      <el-button type="primary" size="small" @click="fetchAndConvert">重试</el-button>
    </div>

    <!-- 内容 -->
    <div v-else-if="htmlContent" class="docx-content" v-html="htmlContent" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Loading, WarningFilled } from '@element-plus/icons-vue'
import mammoth from 'mammoth'
import request from '@/utils/request'

const props = defineProps({
  fileId: { type: [Number, String], required: true },
  fileName: { type: String, default: '' }
})

const loading = ref(true)
const error = ref('')
const htmlContent = ref('')

async function fetchAndConvert() {
  loading.value = true
  error.value = ''
  htmlContent.value = ''

  try {
    const arrayBuffer = await request.get(`/files/${props.fileId}`, {
      responseType: 'arraybuffer'
    })

    const result = await mammoth.convertToHtml({ arrayBuffer })

    if (result.messages && result.messages.length > 0) {
      const warnings = result.messages.filter(m => m.type === 'warning')
      if (warnings.length > 0) {
        console.warn('[WordPreview] mammoth warnings:', warnings.map(w => w.message))
      }
    }

    htmlContent.value = result.value || ''
    if (!htmlContent.value.trim()) {
      error.value = '文档内容为空或无法识别'
    }
  } catch (e) {
    console.error('[WordPreview] conversion failed:', e)
    error.value = '文档转换失败，文件可能已损坏或格式不支持'
  } finally {
    loading.value = false
  }
}

watch(() => props.fileId, () => {
  if (props.fileId) fetchAndConvert()
}, { immediate: true })
</script>

<style scoped>
.docx-viewer {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.docx-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #888;
}

.docx-state p {
  font-size: 15px;
  color: #999;
}

.docx-spin {
  animation: docx-spin 1s linear infinite;
  color: #666;
}

@keyframes docx-spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

.docx-content {
  flex: 1;
  overflow-y: auto;
  padding: 32px 48px;
  background: #ffffff;
  color: #1a1a1a;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 15px;
  line-height: 1.8;
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
  word-break: break-word;
}

/* :deep() 样式覆盖 mammoth 输出的 HTML — 白底黑字 */
.docx-content :deep(h1) { font-size: 26px; font-weight: 700; margin: 20px 0 14px; color: #1a1a1a; border-bottom: 1px solid #e0e0e0; padding-bottom: 8px; }
.docx-content :deep(h2) { font-size: 22px; font-weight: 600; margin: 18px 0 12px; color: #2a2a2a; }
.docx-content :deep(h3) { font-size: 18px; font-weight: 600; margin: 14px 0 10px; color: #333; }
.docx-content :deep(h4) { font-size: 16px; font-weight: 600; margin: 12px 0 8px; color: #444; }
.docx-content :deep(p)  { margin: 8px 0; }
.docx-content :deep(strong) { color: #1a1a1a; }

.docx-content :deep(table) {
  border-collapse: collapse;
  margin: 14px 0;
  width: 100%;
  font-size: 14px;
}

.docx-content :deep(td),
.docx-content :deep(th) {
  border: 1px solid #d0d0d0;
  padding: 8px 14px;
  vertical-align: top;
}

.docx-content :deep(th) {
  background: #f5f5f5;
  font-weight: 600;
  color: #1a1a1a;
}

.docx-content :deep(ul),
.docx-content :deep(ol) {
  padding-left: 28px;
  margin: 8px 0;
}

.docx-content :deep(li) {
  margin: 4px 0;
}

.docx-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
  margin: 10px 0;
}

.docx-content :deep(blockquote) {
  border-left: 3px solid #ccc;
  padding-left: 16px;
  margin: 12px 0;
  color: #666;
}

.docx-content :deep(code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px;
}

.docx-content :deep(pre) {
  background: #f5f5f5;
  padding: 14px 20px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}
</style>
