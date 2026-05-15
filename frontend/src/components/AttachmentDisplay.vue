<template>
  <div>
    <!-- ========== 附件缩略图网格 -- File list mode (分组展示) ========== -->
    <template v-if="files && files.length > 0">
      <!-- 全局操作栏 -->
      <div v-if="showBatchActions && files.length > 0" class="attach-toolbar">
        <el-checkbox
          :model-value="allSelected"
          :indeterminate="someSelected && !allSelected"
          @change="toggleAll"
        >全选</el-checkbox>
        <span v-if="selectedIds.length > 0" class="attach-selected-count">
          已选中 {{ selectedIds.length }} 个文件
        </span>
        <div class="attach-toolbar-actions">
          <el-button
            v-if="selectedIds.length > 0"
            type="primary"
            size="small"
            @click="downloadSelected"
          >
            <el-icon><Download /></el-icon> 下载选中 ({{ selectedIds.length }})
          </el-button>
        </div>
      </div>

      <!-- 按材料类型分组 -->
      <div v-for="(group, gIdx) in fileGroups" :key="'fg-' + gIdx" class="attach-group">
        <div class="attach-group-header">
          <span class="attach-group-title">{{ group.label }}</span>
          <span class="attach-group-count">（{{ group.files.length }}个文件）</span>
        </div>
        <div class="attachment-grid">
          <div v-for="file in group.files" :key="file.id" class="attachment-item">
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
              <span class="attachment-name" :title="'原始文件名：' + (file.originalName || '未知')">
                {{ displayName(file) }}
              </span>
              <div class="attachment-actions">
                <el-button
                  v-if="canPreview(file)"
                  type="primary" link size="small"
                  @click="openFilePreview(file)"
                >预览</el-button>
                <el-button type="primary" link size="small" @click="downloadFile(file)">
                  <el-icon><Download /></el-icon> 下载
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ========== 附件缩略图网格 -- URL list mode (分组展示) ========== -->
    <template v-else-if="fileUrls && fileUrls.length > 0">
      <div v-if="showBatchActions && fileUrls.length > 0" class="attach-toolbar">
        <el-checkbox
          :model-value="allSelected"
          :indeterminate="someSelected && !allSelected"
          @change="toggleAll"
        >全选</el-checkbox>
        <span v-if="selectedUrlIdxs.length > 0" class="attach-selected-count">
          已选中 {{ selectedUrlIdxs.length }} 个文件
        </span>
        <div class="attach-toolbar-actions">
          <el-button
            v-if="selectedUrlIdxs.length > 0"
            type="primary"
            size="small"
            @click="downloadSelected"
          >
            <el-icon><Download /></el-icon> 下载选中 ({{ selectedUrlIdxs.length }})
          </el-button>
        </div>
      </div>

      <div v-for="(group, gIdx) in urlFileGroups" :key="'ug-' + gIdx" class="attach-group">
        <div class="attach-group-header">
          <span class="attach-group-title">{{ group.label }}</span>
          <span class="attach-group-count">（{{ group.files.length }}个文件）</span>
        </div>
        <div class="attachment-grid">
          <div v-for="file in group.files" :key="file.id || file.url" class="attachment-item">
            <el-checkbox
              v-if="showBatchActions"
              :model-value="selectedUrlIdxs.includes(file._idx)"
              class="attach-check"
              @change="(val) => toggleUrlIdx(file._idx, val)"
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
              <span class="attachment-name" :title="'原始文件名：' + (file.originalName || '未知')">
                {{ displayName(file) }}
              </span>
              <div class="attachment-actions">
                <el-button
                  v-if="canPreviewUrl(file)"
                  type="primary" link size="small"
                  @click="openUrlPreview(file._idx)"
                >预览</el-button>
                <el-button type="primary" link size="small" @click="downloadUrlFile(file)">
                  <el-icon><Download /></el-icon> 下载
                </el-button>
              </div>
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
      :naming-params="getNamingParams()"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Download, VideoCamera, Picture } from '@element-plus/icons-vue'
import FullscreenPreview from '@/components/FullscreenPreview.vue'
import { downloadSelectedFiles, downloadSingleFile } from '@/api/file'
import { generateFileName, generateBatchZipName } from '@/utils/fileNaming'

const props = defineProps({
  files: { type: Array, default: null },
  fileUrls: { type: Array, default: null },
  contextLabel: { type: String, default: '' },
  /** 成果类型，用于下载全部 + ZIP 命名 */
  relatedType: { type: String, default: '' },
  /** 成果 ID，用于下载全部 */
  relatedId: { type: [Number, String], default: null },
  /** 成果名称，用于 ZIP 命名 */
  achievementName: { type: String, default: '' },
  /** 申报人，用于 ZIP 命名 */
  applicantName: { type: String, default: '' },
  /** 是否显示批量操作栏 */
  showBatchActions: { type: Boolean, default: true },
  /** 文件 ID → 材料类型 映射，如 { 27: "获奖证书", 31: "现场合影" } */
  materialMap: { type: Object, default: () => ({}) }
})

// ========== 分组展示 ==========
/** files 模式下的分组 */
const fileGroups = computed(() => {
  return groupByMaterial(props.files || [])
})

/** fileUrls 模式下的分组（附加 _idx 追踪原始索引） */
const urlFileGroups = computed(() => {
  const indexed = (props.fileUrls || []).map((f, i) => ({ ...f, _idx: i }))
  return groupByMaterial(indexed)
})

function groupByMaterial(list) {
  const map = new Map()
  for (const item of list) {
    const label = getMaterialLabel(item)
    if (!map.has(label)) map.set(label, [])
    map.get(label).push(item)
  }
  const result = []
  for (const [label, files] of map) {
    result.push({ label, files })
  }
  return result
}

// ========== 全屏预览 ==========
const previewVisible = ref(false)
const previewFiles = ref([])
const previewIndex = ref(0)

function openFilePreview(file) {
  previewFiles.value = [{
    id: file.id,
    url: getFileUrl(file.id),
    originalName: file.originalName,
    fileType: file.fileType
  }]
  previewIndex.value = 0
  previewVisible.value = true
}

function openUrlPreview(idx) {
  const source = props.fileUrls || []
  previewFiles.value = source.map(f => ({
    id: f.id,
    url: f.id ? getFileUrl(f.id) : fullUrl(f.url),
    originalName: f.originalName,
    fileType: f.fileType || detectFileTypeFromName(f.originalName || f.url || '')
  }))
  previewIndex.value = Math.max(0, Math.min(idx, previewFiles.value.length - 1))
  previewVisible.value = true
}

function openPreview(mode, idx) {
  openFilePreview((props.files || [])[idx] || {})
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
  if (ids.length === 0 && selectedUrlIdxs.value.length > 0 && props.fileUrls) {
    ids = selectedUrlIdxs.value.map(i => props.fileUrls[i]?.id).filter(Boolean)
  }
  if (ids.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }

  const count = ids.length
  if (count > 10) {
    ElMessage.info(`正在打包文件，请稍候…（共 ${count} 个文件）`)
  }

  try {
    const body = {
      fileIds: ids,
      typeName: props.relatedType
        ? ({ COMPETITION: '竞赛', PROJECT: '项目', PAPER: '论文', SOFTWARE: '软著' })[props.relatedType] || props.relatedType
        : '',
      applicantName: props.applicantName || '',
      achievementName: props.achievementName || ''
    }
    const blob = await downloadSelectedFiles(body)
    const zipName = generateBatchZipName(getNamingParams())
    triggerBlobDownload(blob, zipName)
    ElMessage.success('下载完成')
  } catch {
    ElMessage.error('打包下载失败')
  }
}

function triggerDownload(url, filename) {
  const link = document.createElement('a')
  link.href = url
  if (filename) link.download = filename
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

/** 从文件名推断 fileType */
function detectFileTypeFromName(name) {
  if (/\.(png|jpg|jpeg|gif|bmp|webp)$/i.test(name)) return 'IMAGE'
  if (/\.(pdf)$/i.test(name)) return 'DOCUMENT'
  if (/\.(doc|docx|xls|xlsx|ppt|pptx|txt)$/i.test(name)) return 'DOCUMENT'
  if (/\.(mp4|avi|mov|webm)$/i.test(name)) return 'VIDEO'
  if (/\.(zip|rar|7z)$/i.test(name)) return 'ARCHIVE'
  return 'OTHER'
}

function canPreview(file) {
  const name = (file.originalName || '').toLowerCase()
  const isImageLike = file.fileType === 'IMAGE'
    || /\.(png|jpg|jpeg|gif|bmp|webp)$/i.test(name)
  return isImageLike
    || name.endsWith('.pdf')
    || /\.(doc|docx|xls|xlsx|ppt|pptx|txt)$/i.test(name)
    || file.fileType === 'VIDEO'
    || /\.(mp4|avi|mov)$/i.test(name)
}

function canPreviewUrl(file) {
  const name = (file.originalName || file.url || '').toLowerCase()
  // 图片：fileType 优先，其次是 URL 或原始文件名扩展名
  const isImageLike = file.fileType === 'IMAGE'
    || isImageUrl(file.url)
    || /\.(png|jpg|jpeg|gif|bmp|webp)$/i.test(name)
  return isImageLike
    || name.endsWith('.pdf')
    || /\.(doc|docx|xls|xlsx|ppt|pptx|txt)$/i.test(name)
    || file.fileType === 'VIDEO'
    || /\.(mp4|avi|mov|webm)$/i.test(name)
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

/** 获取材料类型标签 */
function getMaterialLabel(file) {
  // 1. 最优先：文件自带 materialType（后端存储的字段）
  if (file.materialType) {
    return file.materialType
  }
  // 2. materialMap prop 明确指定
  if (file.id && props.materialMap[file.id]) {
    return props.materialMap[file.id]
  }
  // 3. 文件名关键词推断
  const name = (file.originalName || '').toLowerCase()
  if (name.includes('获奖证书') || name.includes('证书颁发')) return '获奖证书'
  if (name.includes('证书') || name.includes('cert')) return '获奖证书'
  if (name.includes('合影') || name.includes('photo') || name.includes('照片')) return '现场合影'
  if (name.includes('申报书') || name.includes('proposal')) return '立项申报书'
  if (name.includes('结题材料') || name.includes('结题报告')) return '结题材料'
  if (name.includes('结题证书') || name.includes('完成证书')) return '结题证书'
  if (name.includes('投稿初稿') || name.includes('初稿') || name.includes('draft')) return '投稿初稿'
  if (name.includes('录用终稿') || name.includes('终稿') || name.includes('final')) return '录用终稿'
  if (name.includes('审稿意见') || name.includes('审稿') || name.includes('review')) return '审稿意见'
  if (name.includes('申报材料') || name.includes('源代码') || name.includes('代码')) return '申报材料'
  if (name.includes('扫描件') || name.includes('扫描') || name.includes('scan')) return '证书扫描件'
  // 4. 无法推断 → 取原始文件名（无扩展名）作为材料类型
  const extIdx = file.originalName ? file.originalName.lastIndexOf('.') : -1
  if (extIdx > 0) {
    const raw = file.originalName.substring(0, extIdx)
    if (raw.length <= 10) return raw
  }
  return '其他材料'
}

/** 获取扩展名 */
function getExt(name) {
  const i = (name || '').lastIndexOf('.')
  return i >= 0 ? name.substring(i) : ''
}

/** 显示名称：只显示材料类型.扩展名，如"获奖证书.png" */
function displayName(file) {
  const material = getMaterialLabel(file)
  const ext = getExt(file.originalName || '')
  return material + ext
}

/** 构建下载命名参数 */
function getNamingParams() {
  return {
    achievementType: props.relatedType || '',
    projectName: props.achievementName || '',
    userName: props.applicantName || ''
  }
}

// ========== 下载 ==========
async function downloadFile(file) {
  if (!file || !file.id) return
  try {
    const blob = await downloadSingleFile(file.id)
    // 下载用完整规范名：竞赛_张三_全国大学生数学竞赛_获奖证书.png
    const downloadName = generateFileName({
      ...getNamingParams(),
      originalName: file.originalName || ''
    })
    triggerBlobDownload(blob, downloadName)
  } catch {
    ElMessage.error('下载失败')
  }
}

async function downloadUrlFile(file) {
  if (!file || !file.id) return
  try {
    const blob = await downloadSingleFile(file.id)
    const downloadName = generateFileName({
      ...getNamingParams(),
      originalName: file.originalName || ''
    })
    triggerBlobDownload(blob, downloadName)
  } catch {
    ElMessage.error('下载失败')
  }
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

.attach-selected-count {
  font-size: 13px;
  color: var(--primary-color);
  font-weight: 500;
}

.attach-toolbar-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.attachment-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 12px;
}

.attach-group {
  margin-bottom: var(--space-lg);
}

.attach-group-header {
  display: flex;
  align-items: baseline;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--primary-color);
}

.attach-group-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
}

.attach-group-count {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin-left: 4px;
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
