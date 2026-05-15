<template>
  <Teleport to="body">
    <transition name="fs-fade">
      <div
        v-if="visible"
        class="fs-overlay"
        tabindex="0"
        @keydown="onKeydown"
        ref="overlayRef"
      >
        <!-- ======== 顶部工具栏 ======== -->
        <div class="fs-topbar">
          <div class="fs-topbar-left">
            <span class="fs-filename">{{ currentFile?.originalName || '文件预览' }}</span>
            <span v-if="totalCount > 1" class="fs-counter">{{ currentIndex + 1 }} / {{ totalCount }}</span>
          </div>
          <div class="fs-topbar-right">
            <el-button text class="fs-tool-btn" @click="downloadCurrent" title="下载">
              <el-icon :size="20"><Download /></el-icon>
            </el-button>
            <el-button text class="fs-tool-btn" @click="close" title="关闭 (ESC)">
              <el-icon :size="22"><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- ======== 中间内容 ======== -->
        <div class="fs-body" @mousedown.self="close">
          <!-- 图片预览 -->
          <template v-if="isImage">
            <div
              class="fs-image-stage"
              @wheel.prevent="onWheel"
              @mousedown="onImageMouseDown"
              @mousemove="onImageMouseMove"
              @mouseup="onImageMouseUp"
              @mouseleave="onImageMouseUp"
            >
              <img
                :src="currentUrl"
                :style="imageTransform"
                class="fs-image"
                :class="{ 'fs-image-grabbing': isDragging }"
                draggable="false"
                @dragstart.prevent
              />

              <!-- 左右箭头 -->
              <div v-if="totalCount > 1 && currentIndex > 0" class="fs-arrow fs-arrow-left" @click.stop="goPrev">
                <el-icon :size="32"><ArrowLeft /></el-icon>
              </div>
              <div v-if="totalCount > 1 && currentIndex < totalCount - 1" class="fs-arrow fs-arrow-right" @click.stop="goNext">
                <el-icon :size="32"><ArrowRight /></el-icon>
              </div>
            </div>
          </template>

          <!-- PDF 预览 -->
          <iframe
            v-else-if="isPdf"
            :src="currentUrl"
            class="fs-pdf-iframe"
          />

          <!-- Word DOCX 预览 (mammoth.js 转 HTML) -->
          <div v-else-if="isWordDocx" class="fs-docx-viewer">
            <WordPreviewModal
              :file-id="currentFile?.id || extractIdFromUrl(currentUrl)"
              :file-name="currentFile?.originalName || ''"
            />
          </div>

          <!-- 旧版 .doc — 不支持在线预览 -->
          <div v-else-if="isLegacyDoc" class="fs-unsupported">
            <el-icon :size="72"><Document /></el-icon>
            <p>旧版 .doc 文档暂不支持在线预览</p>
            <p class="fs-hint">请下载后使用 Word 查看</p>
            <el-button type="primary" @click="downloadCurrent">下载文件</el-button>
          </div>

          <!-- 其他 Office (XLS/XLSX/PPT/PPTX) -->
          <div v-else-if="isOfficeDoc" class="fs-unsupported">
            <el-icon :size="72"><Document /></el-icon>
            <p>Office 文档暂不支持在线预览</p>
            <p class="fs-hint">请下载后使用对应软件查看</p>
            <el-button type="primary" @click="downloadCurrent">下载文件</el-button>
          </div>

          <!-- 文本预览 -->
          <div v-else-if="isTxt" class="fs-text-viewer">
            <pre>{{ textContent }}</pre>
          </div>

          <!-- 视频预览 -->
          <div v-else-if="isVideo" class="fs-video-stage">
            <video
              ref="videoRef"
              :src="currentUrl"
              controls
              class="fs-video"
              autoplay
              playsinline
              @loadedmetadata="onVideoLoaded"
              @play="onVideoPlay"
              @pause="onVideoPause"
            >
              您的浏览器不支持视频播放
            </video>
            <!-- 自定义覆盖控制提示 -->
            <div class="fs-video-hint">空格键 播放/暂停 | F 全屏 | ← → 切换文件</div>
          </div>

          <!-- ZIP / 不支持预览 -->
          <div v-else class="fs-unsupported">
            <el-icon :size="72"><WarningFilled /></el-icon>
            <p>{{ isZip ? 'ZIP 压缩包无法在线预览，请下载后查看' : '此文件格式不支持预览' }}</p>
            <el-button type="primary" @click="downloadCurrent">下载文件</el-button>
          </div>
        </div>

        <!-- ======== 底部工具栏 ======== -->
        <div v-if="isImage" class="fs-bottombar">
          <div class="fs-zoom-controls">
            <el-button text class="fs-tool-btn" @click="zoomOut" title="缩小 (-)">
              <el-icon :size="18"><ZoomOut /></el-icon>
            </el-button>
            <span class="fs-zoom-label">{{ Math.round(scale * 100) }}%</span>
            <el-button text class="fs-tool-btn" @click="zoomIn" title="放大 (+)">
              <el-icon :size="18"><ZoomIn /></el-icon>
            </el-button>
            <el-divider direction="vertical" />
            <el-button text class="fs-tool-btn" @click="resetZoom" title="重置 (0)">
              <el-icon :size="16"><RefreshLeft /></el-icon>
            </el-button>
          </div>
          <div v-if="totalCount > 1" class="fs-nav-controls">
            <el-button text class="fs-tool-btn" :disabled="currentIndex <= 0" @click="goPrev" title="上一张 (←)">
              <el-icon :size="18"><ArrowLeft /></el-icon>
            </el-button>
            <span class="fs-zoom-label">{{ currentIndex + 1 }} / {{ totalCount }}</span>
            <el-button text class="fs-tool-btn" :disabled="currentIndex >= totalCount - 1" @click="goNext" title="下一张 (→)">
              <el-icon :size="18"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount, nextTick } from 'vue'
import {
  Download, Close, ArrowLeft, ArrowRight,
  ZoomIn, ZoomOut, RefreshLeft, WarningFilled, Document
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import WordPreviewModal from '@/components/WordPreviewModal.vue'
import { generateFileName } from '@/utils/fileNaming'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** FileInfo 数组 {id, url, originalName, fileType} */
  files: { type: Array, default: () => [] },
  /** 起始索引 */
  initialIndex: { type: Number, default: 0 },
  /** 下载文件名上下文 */
  contextLabel: { type: String, default: '' },
  /** 命名参数 { achievementType, projectName, userName } */
  namingParams: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'close'])

// ========== 核心状态 ==========
const visible = ref(false)
const currentIndex = ref(0)
const overlayRef = ref(null)
const videoRef = ref(null)

const totalCount = computed(() => props.files.length)
const currentFile = computed(() => props.files[currentIndex.value] || null)
const currentUrl = computed(() => {
  if (!currentFile.value) return ''
  return currentFile.value.url || `/api/files/${currentFile.value.id}`
})

const isImage = computed(() => {
  const f = currentFile.value
  if (!f) return false
  return (f.fileType === 'IMAGE') || /\.(png|jpg|jpeg|gif|bmp|webp)$/i.test(f.url || f.originalName || '')
})

const isPdf = computed(() => {
  const name = (currentFile.value?.originalName || currentFile.value?.url || '').toLowerCase()
  return name.endsWith('.pdf')
})

const isWordDocx = computed(() => {
  const name = (currentFile.value?.originalName || currentFile.value?.url || '').toLowerCase()
  return name.endsWith('.docx')
})

const isLegacyDoc = computed(() => {
  const name = (currentFile.value?.originalName || currentFile.value?.url || '').toLowerCase()
  return name.endsWith('.doc')
})

const isOfficeDoc = computed(() => {
  const name = (currentFile.value?.originalName || currentFile.value?.url || '').toLowerCase()
  return /\.(xls|xlsx|ppt|pptx)$/i.test(name)
})

const isTxt = computed(() => {
  const name = (currentFile.value?.originalName || currentFile.value?.url || '').toLowerCase()
  return name.endsWith('.txt')
})

const isVideo = computed(() => {
  const f = currentFile.value
  if (f?.fileType === 'VIDEO') return true
  const name = (f?.originalName || f?.url || '').toLowerCase()
  return /\.(mp4|avi|mov|wmv|flv|webm)$/i.test(name)
})

const isZip = computed(() => {
  const f = currentFile.value
  if (f?.fileType === 'ARCHIVE') return true
  const name = (f?.originalName || f?.url || '').toLowerCase()
  return /\.(zip|rar|7z|tar|gz)$/i.test(name)
})

// 文本预览内容
const textContent = ref('')
let textFetchAbort = null

// Office 文档回退提示
const showOfficeFallback = ref(false)
const showDocxFallback = ref(false)

/** 从 URL 中提取文件 ID，如 /api/files/27 → 27 */
function extractIdFromUrl(url) {
  const match = (url || '').match(/\/files\/(\d+)/)
  return match ? parseInt(match[1]) : 0
}

/** Office Online 预览 URL — 本地 localhost 不可用，回退 mammoth */
const officeOnlineUrl = computed(() => {
  const url = currentUrl.value
  if (!url) return ''
  if (url.includes('localhost') || url.includes('127.0.0.1')) return ''
  const fullUrl = url.startsWith('http') ? url : (window.location.origin + url)
  return `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(fullUrl)}`
})

// ========== 图片缩放 / 拖拽 ==========
const scale = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)
const isDragging = ref(false)
let dragStartX = 0, dragStartY = 0
let dragStartOffsetX = 0, dragStartOffsetY = 0

const imageTransform = computed(() => ({
  transform: `translate(${offsetX.value}px, ${offsetY.value}px) scale(${scale.value})`,
  cursor: isDragging.value ? 'grabbing' : scale.value > 1 ? 'grab' : 'default',
  transition: isDragging.value ? 'none' : 'transform 0.15s ease'
}))

function onWheel(e) {
  const rect = e.currentTarget.getBoundingClientRect()
  const mouseX = e.clientX - rect.left
  const mouseY = e.clientY - rect.top

  const delta = e.deltaY < 0 ? 0.15 : -0.15
  const oldScale = scale.value
  const newScale = Math.max(0.3, Math.min(6, oldScale + delta))

  // 以鼠标位置为中心缩放
  const ratio = newScale / oldScale
  offsetX.value = mouseX - (mouseX - offsetX.value) * ratio
  offsetY.value = mouseY - (mouseY - offsetY.value) * ratio
  scale.value = newScale
}

function onImageMouseDown(e) {
  if (e.button !== 0) return
  isDragging.value = true
  dragStartX = e.clientX
  dragStartY = e.clientY
  dragStartOffsetX = offsetX.value
  dragStartOffsetY = offsetY.value
}

function onImageMouseMove(e) {
  if (!isDragging.value) return
  offsetX.value = dragStartOffsetX + e.clientX - dragStartX
  offsetY.value = dragStartOffsetY + e.clientY - dragStartY
}

function onImageMouseUp() {
  isDragging.value = false
}

function zoomIn()  { scale.value = Math.min(6, scale.value + 0.25) }
function zoomOut() { scale.value = Math.max(0.3, scale.value - 0.25) }
function resetZoom() { scale.value = 1; offsetX.value = 0; offsetY.value = 0 }

// ========== 导航 ==========
function goPrev() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    resetZoom()
  }
}
function goNext() {
  if (currentIndex.value < totalCount.value - 1) {
    currentIndex.value++
    resetZoom()
  }
}

// ========== 下载 ==========
async function downloadCurrent() {
  const file = currentFile.value
  if (!file || !file.id) return

  const downloadName = generateFileName({
    ...props.namingParams,
    originalName: file.originalName || ''
  })

  try {
    const blob = await request.get(`/files/${file.id}/download`, {
      responseType: 'blob'
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = downloadName
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch {
    const url = `/api/files/${file.id}/download`
    const link = document.createElement('a')
    link.href = url
    link.download = downloadName
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
}

// ========== 视频控制 ==========
function onVideoLoaded() {}
function onVideoPlay() {}
function onVideoPause() {}

function toggleVideoPlay() {
  const video = videoRef.value
  if (!video) return
  if (video.paused) {
    video.play()
  } else {
    video.pause()
  }
}

function toggleVideoFullscreen() {
  const video = videoRef.value
  if (!video) return
  if (video.requestFullscreen) {
    video.requestFullscreen()
  } else if (video.webkitRequestFullscreen) {
    video.webkitRequestFullscreen()
  }
}

// ========== 关闭 ==========
function close() {
  visible.value = false
  emit('update:modelValue', false)
  emit('close')
}

// ========== 键盘 ==========
function onKeydown(e) {
  switch (e.key) {
    case 'Escape':
      e.preventDefault()
      close()
      break
    case 'ArrowLeft':
      e.preventDefault()
      goPrev()
      break
    case 'ArrowRight':
      e.preventDefault()
      goNext()
      break
    case '+':
    case '=':
      e.preventDefault()
      zoomIn()
      break
    case '-':
      e.preventDefault()
      zoomOut()
      break
    case '0':
      e.preventDefault()
      resetZoom()
      break
    case ' ':
      if (isVideo.value) {
        e.preventDefault()
        toggleVideoPlay()
      }
      break
    case 'f':
    case 'F':
      if (isVideo.value) {
        e.preventDefault()
        toggleVideoFullscreen()
      }
      break
  }
}

// ========== 文本文件读取 ==========
async function fetchTextContent(url) {
  if (textFetchAbort) textFetchAbort = false
  textContent.value = ''
  try {
    const res = await request.get(url.startsWith('/api') ? url : new URL(url).pathname, {
      responseType: 'text'
    })
    if (typeof res === 'string') {
      textContent.value = res
    } else {
      textContent.value = '[无法读取文本内容]'
    }
  } catch {
    textContent.value = '[文本文件读取失败]'
  }
}

// 监听当前文件变化，自动加载 TXT 内容
watch(currentFile, (file) => {
  showOfficeFallback.value = false
  showDocxFallback.value = false
  if (file && isTxt.value) {
    fetchTextContent(currentUrl.value)
  } else {
    textContent.value = ''
  }
})

// ========== 生命周期 ==========
watch(() => props.modelValue, (val) => {
  if (val) {
    currentIndex.value = Math.max(0, Math.min(props.initialIndex, totalCount.value - 1))
    resetZoom()
    showOfficeFallback.value = false
    showDocxFallback.value = false
    visible.value = true
    // 防止背景滚动
    document.body.style.overflow = 'hidden'
    nextTick(() => overlayRef.value?.focus())
  } else {
    visible.value = false
    document.body.style.overflow = ''
    textContent.value = ''
  }
}, { immediate: true })

onBeforeUnmount(() => {
  document.body.style.overflow = ''
})
</script>

<style scoped>
/* ======== 覆盖层 ======== */
.fs-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: #1a1a1a;
  display: flex;
  flex-direction: column;
  outline: none;
  user-select: none;
  -webkit-user-select: none;
}

/* ======== 顶部栏 ======== */
.fs-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px;
  padding: 0 16px;
  background: rgba(0, 0, 0, 0.55);
  color: #eee;
  flex-shrink: 0;
  z-index: 10;
}
.fs-topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.fs-filename {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fs-counter {
  font-size: 13px;
  color: #aaa;
  white-space: nowrap;
}
.fs-topbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 工具按钮 */
.fs-tool-btn {
  color: #ccc !important;
  border-radius: 6px;
  transition: background 0.15s, color 0.15s;
}
.fs-tool-btn:hover {
  color: #fff !important;
  background: rgba(255,255,255,0.12) !important;
}
.fs-tool-btn.is-disabled {
  color: #555 !important;
  cursor: not-allowed;
}

/* ======== 主体内容 ======== */
.fs-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

/* --- 图片舞台 --- */
.fs-image-stage {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}
.fs-image {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  cursor: grab;
}
.fs-image-grabbing {
  cursor: grabbing;
}

/* --- 左右箭头 --- */
.fs-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255,255,255,0.1);
  color: #fff;
  cursor: pointer;
  z-index: 5;
  transition: background 0.15s;
}
.fs-arrow:hover  { background: rgba(255,255,255,0.25); }
.fs-arrow-left  { left: 24px; }
.fs-arrow-right { right: 24px; }

/* --- PDF iframe --- */
.fs-pdf-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #525659;
}

/* --- DOCX viewer --- */
.fs-docx-viewer {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* --- 文本预览 --- */
.fs-text-viewer {
  width: 100%;
  height: 100%;
  overflow: auto;
  padding: 24px 40px;
  background: #1e1e2e;
}
.fs-text-viewer pre {
  color: #cdd6f4;
  font-family: 'Cascadia Code', 'Fira Code', 'JetBrains Mono', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  max-width: 900px;
  margin: 0 auto;
}

/* --- 视频 --- */
.fs-video-stage {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
}
.fs-video {
  max-width: 100%;
  max-height: 100%;
  outline: none;
}

.fs-video-hint {
  position: absolute;
  bottom: 64px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 13px;
  color: rgba(255,255,255,0.45);
  pointer-events: none;
  transition: opacity 0.6s;
  opacity: 1;
}

/* --- 不支持预览 --- */
.fs-unsupported {
  text-align: center;
  color: #888;
}
.fs-unsupported .el-icon { margin-bottom: 12px; color: #666; }
.fs-unsupported p { font-size: 16px; margin: 8px 0 20px; }
.fs-unsupported .fs-hint { font-size: 13px; color: #666; margin-top: -12px; }

/* ======== 底部栏 ======== */
.fs-bottombar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  height: 50px;
  padding: 0 16px;
  background: rgba(0, 0, 0, 0.55);
  flex-shrink: 0;
  z-index: 10;
}
.fs-zoom-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}
.fs-nav-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}
.fs-zoom-label {
  font-size: 13px;
  color: #ccc;
  min-width: 42px;
  text-align: center;
}

/* ======== 过渡动画 ======== */
.fs-fade-enter-active,
.fs-fade-leave-active {
  transition: opacity 0.2s ease;
}
.fs-fade-enter-from,
.fs-fade-leave-to {
  opacity: 0;
}
</style>
