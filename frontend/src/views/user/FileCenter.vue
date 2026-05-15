<template>
  <div>
    <div class="page-header">
      <h2>文件管理</h2>
      <p class="header-desc">您上传的所有文件，支持预览、下载和删除</p>
    </div>

    <!-- 统计概要：单行紧凑布局，非卡片网格 -->
    <div class="summary-bar">
      <div class="summary-item">
        <span class="summary-value">{{ allFiles.length }}</span>
        <span class="summary-unit">个文件</span>
      </div>
      <span class="summary-divider" />
      <div class="summary-item">
        <span class="summary-value">{{ formatFileSize(totalStorage) }}</span>
      </div>
      <span class="summary-divider" />
      <template v-for="(count, type) in typeBreakdown" :key="type">
        <div class="summary-item summary-item--minor">
          <span class="summary-value summary-value--minor">{{ count }}</span>
          <span class="summary-unit">{{ fileTypeLabel(type) }}</span>
        </div>
      </template>
    </div>

    <!-- 筛选区 -->
    <div class="card-wrapper">
      <el-card>
        <el-form :inline="true" :model="filters" size="default">
          <el-form-item label="成果类型">
            <el-select v-model="filters.relatedType" placeholder="全部" clearable style="width:130px" @change="applyFilters">
              <el-option label="学科竞赛" value="COMPETITION" />
              <el-option label="创新项目" value="PROJECT" />
              <el-option label="学术论文" value="PAPER" />
              <el-option label="软件著作" value="SOFTWARE" />
            </el-select>
          </el-form-item>
          <el-form-item label="文件类型">
            <el-select v-model="filters.fileType" placeholder="全部" clearable style="width:120px" @change="applyFilters">
              <el-option label="图片" value="IMAGE" />
              <el-option label="文档" value="DOCUMENT" />
              <el-option label="压缩包" value="ARCHIVE" />
              <el-option label="视频" value="VIDEO" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序">
            <el-select v-model="filters.sortBy" style="width:120px" @change="applyFilters">
              <el-option label="上传时间" value="time" />
              <el-option label="文件名" value="name" />
              <el-option label="文件大小" value="size" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 批量操作栏 -->
    <div v-if="selectedIds.length > 0" class="batch-bar">
      <span class="batch-info">已选中 <strong>{{ selectedIds.length }}</strong> 个文件</span>
      <el-button size="small" @click="handleBatchDelete">
        <el-icon><Delete /></el-icon> 批量删除
      </el-button>
    </div>

    <!-- 文件表格 -->
    <el-card>
      <el-table
        :data="displayFiles"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        ref="tableRef"
        row-key="id"
        empty-text=" "
      >
        <el-table-column type="selection" width="40" />
        <el-table-column label="文件名" min-width="240">
          <template #default="{ row }">
            <button class="file-link" @click="handlePreview(row)">
              {{ row.originalName }}
            </button>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <span class="type-badge" :class="'type-badge--' + (row.fileType || 'OTHER').toLowerCase()">
              {{ fileTypeLabel(row.fileType) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="材料分类" width="110">
          <template #default="{ row }">{{ row.materialType || '-' }}</template>
        </el-table-column>
        <el-table-column label="关联成果" width="180">
          <template #default="{ row }">
            <template v-if="row.relatedType && row.relatedId">
              <button class="file-link" @click="goToAchievement(row)">
                {{ typeLabel(row.relatedType) }} #{{ row.relatedId }}
              </button>
            </template>
            <span v-else class="text-secondary">未关联</span>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="90" align="right">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">{{ formatTime(row.uploadTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button type="primary" link size="small" @click="handlePreview(row)">预览</el-button>
              <el-button type="primary" link size="small" @click="handleDownload(row)">下载</el-button>
              <el-button type="danger" link size="small" @click="handleDeleteRow(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <div v-if="!loading && displayFiles.length === 0" class="empty-state">
        <div class="empty-icon">
          <el-icon :size="36"><Folder /></el-icon>
        </div>
        <p class="empty-title">暂无文件</p>
        <p class="empty-desc">提交成果时上传的文件会显示在这里</p>
      </div>
    </el-card>

    <!-- 全屏预览 -->
    <FullscreenPreview
      v-model="previewVisible"
      :files="previewFiles"
      :initial-index="previewIndex"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Folder } from '@element-plus/icons-vue'
import { getMyFiles, deleteFile, batchDeleteFiles, downloadSingleFile, getFileUrl } from '@/api/file'
import { typeLabel } from '@/utils/statusMap'
import { downloadBlob, generateFileName } from '@/utils/fileNaming'
import FullscreenPreview from '@/components/FullscreenPreview.vue'

const router = useRouter()
const loading = ref(false)
const allFiles = ref([])
const selectedIds = ref([])
const tableRef = ref(null)

const filters = reactive({
  fileType: '',
  relatedType: '',
  sortBy: 'time'
})

const totalStorage = computed(() => allFiles.value.reduce((sum, f) => sum + (f.fileSize || 0), 0))

const typeBreakdown = computed(() => {
  const map = {}
  allFiles.value.forEach(f => {
    const t = f.fileType || 'OTHER'
    map[t] = (map[t] || 0) + 1
  })
  return map
})

const displayFiles = computed(() => {
  let files = [...allFiles.value]
  if (filters.fileType) files = files.filter(f => f.fileType === filters.fileType)
  if (filters.relatedType) files = files.filter(f => f.relatedType === filters.relatedType)
  if (filters.sortBy === 'name') {
    files.sort((a, b) => (a.originalName || '').localeCompare(b.originalName || ''))
  } else if (filters.sortBy === 'size') {
    files.sort((a, b) => (a.fileSize || 0) - (b.fileSize || 0))
  } else {
    files.sort((a, b) => new Date(b.uploadTime || 0) - new Date(a.uploadTime || 0))
  }
  return files
})

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getMyFiles({
      fileType: filters.fileType || undefined,
      relatedType: filters.relatedType || undefined,
      sortBy: filters.sortBy
    })
    if (res.code === 200) {
      allFiles.value = res.data || []
    }
  } catch {
    ElMessage.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

function applyFilters() { fetchData() }

function resetFilters() {
  filters.fileType = ''
  filters.relatedType = ''
  filters.sortBy = 'time'
  fetchData()
}

function goToAchievement(row) {
  const typeMap = {
    COMPETITION: 'competition',
    PROJECT: 'project',
    PAPER: 'paper',
    SOFTWARE: 'software'
  }
  const prefix = typeMap[row.relatedType]
  if (prefix && row.relatedId) {
    router.push(`/${prefix}/detail/${row.relatedId}`)
  }
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

async function handleDeleteRow(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除 "${row.originalName}"？${row.relatedType ? '该文件关联了成果，删除后无法恢复。' : ''}`,
      '确认删除',
      { type: 'warning' }
    )
    const res = await deleteFile(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch { /* cancel */ }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 个文件？此操作不可恢复。`,
      '批量删除',
      { type: 'warning' }
    )
    const res = await batchDeleteFiles(selectedIds.value)
    if (res.code === 200) {
      ElMessage.success(`成功删除 ${res.data.deleted} 个文件`)
      selectedIds.value = []
      fetchData()
    } else {
      ElMessage.error(res.message || '批量删除失败')
    }
  } catch { /* cancel */ }
}

// ========== 预览 ==========
const previewVisible = ref(false)
const previewFiles = ref([])
const previewIndex = ref(0)

function handlePreview(row) {
  previewFiles.value = [{
    id: row.id,
    url: getFileUrl(row.id),
    originalName: row.originalName,
    fileType: row.fileType
  }]
  previewIndex.value = 0
  previewVisible.value = true
}

// ========== 下载 ==========
async function handleDownload(row) {
  try {
    const res = await downloadSingleFile(row.id)
    // 检查是否返回了错误 JSON（而非文件 blob）
    if (res instanceof Blob && res.type === 'application/json') {
      const text = await res.text()
      const json = JSON.parse(text)
      ElMessage.error(json.message || '下载失败')
      return
    }
    const fileName = generateFileName({
      achievementType: row.relatedType || '',
      originalName: row.originalName,
      materialType: row.materialType,
      userName: row.uploaderName
    })
    downloadBlob(res, fileName)
  } catch (e) {
    ElMessage.error('下载失败: ' + (e?.message || '网络错误'))
  }
}

// ========== 工具函数 ==========
function fileTypeLabel(type) {
  const map = { IMAGE: '图片', DOCUMENT: '文档', ARCHIVE: '压缩包', VIDEO: '视频', OTHER: '其他' }
  return map[type] || type
}

function formatFileSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(i === 0 ? 0 : 1) + ' ' + units[i]
}

function formatTime(time) {
  if (!time) return '-'
  return time.substring(0, 19).replace('T', ' ')
}
</script>

<style scoped>
/* ========== 页面标题 ========== */
.page-header {
  margin-bottom: var(--space-md);
}

.page-header h2 {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.header-desc {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0;
}

/* ========== 统计概要 ========== */
.summary-bar {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
  padding: 12px 0;
}

.summary-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.summary-value {
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
  letter-spacing: -0.02em;
}

.summary-value--minor {
  font-size: var(--text-lg);
  font-weight: 600;
}

.summary-unit {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  font-weight: 500;
}

.summary-item--minor .summary-unit {
  font-size: var(--text-xs);
}

.summary-divider {
  width: 1px;
  height: 28px;
  background: var(--border-light);
  flex-shrink: 0;
}

/* ========== 批量操作栏 ========== */
.batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  margin-bottom: var(--space-md);
  background: var(--primary-light);
  border-radius: var(--card-radius);
  font-size: var(--text-sm);
  color: var(--text-regular);
}

.batch-bar strong {
  color: var(--primary-color);
  font-weight: 600;
}

.batch-info {
  font-size: var(--text-sm);
}

/* ========== 表格内文件链接 ========== */
.file-link {
  background: none;
  border: none;
  padding: 0;
  font-size: var(--text-sm);
  color: var(--primary-color);
  cursor: pointer;
  font-family: inherit;
}

.file-link:hover {
  text-decoration: underline;
}

/* ========== 类型标记 ========== */
.type-badge {
  display: inline-block;
  font-size: var(--text-xs);
  font-weight: 500;
  color: var(--text-secondary);
}

.type-badge--image  { color: var(--primary-color); }
.type-badge--document { color: var(--success-color); }
.type-badge--archive { color: var(--warning-color); }
.type-badge--video  { color: var(--danger-color); }
.type-badge--other  { color: var(--info-color); }

/* ========== 操作列 ========== */
.row-actions {
  display: flex;
  align-items: center;
  gap: 0;
}

.text-secondary {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

/* ========== 空状态 ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-2xl) 0;
}

.empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--hover-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  margin-bottom: var(--space-md);
}

.empty-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.empty-desc {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0;
}
</style>
