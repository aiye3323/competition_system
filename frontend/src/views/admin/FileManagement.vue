<template>
  <div>
    <div class="page-header">
      <h2>文件管理</h2>
      <el-button type="primary" @click="uploadVisible = true">上传文件</el-button>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: var(--space-lg);">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-card-value">{{ stats.totalFiles }}</div>
          <div class="stat-card-label">文件总数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-card-value">{{ formatFileSize(stats.totalStorage) }}</div>
          <div class="stat-card-label">存储总量</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-card-value">{{ stats.recentUploads }}</div>
          <div class="stat-card-label">近7日上传</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-card-tags">
            <el-tag v-for="(count, type) in stats.countByType" :key="type"
              :type="fileTypeTag(type)" size="small" style="margin:2px;">
              {{ fileTypeLabel(type) }} {{ count }}
            </el-tag>
            <span v-if="!stats.countByType || Object.keys(stats.countByType).length === 0"
              class="text-secondary" style="font-size:var(--text-xs);">暂无</span>
          </div>
          <div class="stat-card-label">类型分布</div>
        </div>
      </el-col>
    </el-row>

    <!-- 筛选区 -->
    <div class="card-wrapper">
      <el-card>
        <el-form :inline="true" :model="filters" size="default">
          <el-form-item label="文件类型">
            <el-select v-model="filters.fileType" placeholder="全部" clearable style="width:120px;">
              <el-option label="图片" value="IMAGE" />
              <el-option label="文档" value="DOCUMENT" />
              <el-option label="压缩包" value="ARCHIVE" />
              <el-option label="视频" value="VIDEO" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="材料类型">
            <el-select v-model="filters.materialType" placeholder="全部" clearable style="width:130px;">
              <el-option v-for="mt in materialTypes" :key="mt" :label="mt" :value="mt" />
            </el-select>
          </el-form-item>
          <el-form-item label="关联类型">
            <el-select v-model="filters.relatedType" placeholder="全部" clearable style="width:120px;">
              <el-option label="学科竞赛" value="COMPETITION" />
              <el-option label="创新项目" value="PROJECT" />
              <el-option label="学术论文" value="PAPER" />
              <el-option label="软件著作" value="SOFTWARE" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="filters.keyword" placeholder="搜索文件名" clearable style="width:160px;"
              @keyup.enter="fetchData" />
          </el-form-item>
          <el-form-item label="上传日期">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
              start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD"
              style="width:240px;" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 表格 -->
    <el-card>
      <div v-if="selectedIds.length > 0" style="margin-bottom:var(--space-md);">
        <el-button type="danger" size="small" @click="handleBatchDelete">
          批量删除 ({{ selectedIds.length }})
        </el-button>
      </div>
      <el-table :data="fileList" stripe v-loading="loading"
        @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column label="文件名" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button type="primary" link @click="handlePreview(row)">
              {{ row.originalName }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="文件类型" width="90">
          <template #default="{ row }">
            <el-tag :type="fileTypeTag(row.fileType)" size="small">{{ fileTypeLabel(row.fileType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="材料类型" width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-regular" style="font-size:var(--text-xs);">{{ row.materialType || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联成果" width="130">
          <template #default="{ row }">
            <template v-if="row.relatedType">
              <span style="font-size:var(--text-xs);">{{ typeLabel(row.relatedType) }}</span>
              <span class="text-secondary" style="font-size:12px;"> #{{ row.relatedId }}</span>
            </template>
            <span v-else class="text-secondary" style="font-size:var(--text-xs);">未关联</span>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="90">
          <template #default="{ row }">
            <span class="text-regular" style="font-size:var(--text-xs);">{{ formatFileSize(row.fileSize) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上传者" width="100" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-size:var(--text-sm);">{{ row.uploaderName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">
            <span style="font-size:var(--text-xs);">{{ formatTime(row.uploadTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handlePreview(row)">预览</el-button>
              <el-button type="primary" link size="small" @click="handleDownload(row)">下载</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="fetchData" />
      </div>
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog v-model="uploadVisible" title="上传文件" width="520px">
      <FileUpload
        :material-type="uploadMaterialType"
        :area-config="uploadAreaConfig"
        @update:file-ids="onUploadSuccess"
      />
    </el-dialog>

    <!-- 全屏预览 -->
    <FullscreenPreview
      v-model="previewVisible"
      :files="previewFiles"
      :initial-index="previewIndex"
      :context-label="previewContextLabel"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getFileList, getFileStats, deleteFile,
  downloadSingleFile, getFileUrl
} from '@/api/file'
import { typeLabel } from '@/utils/statusMap'
import { downloadBlob } from '@/utils/fileNaming'
import FileUpload from '@/components/FileUpload.vue'
import FullscreenPreview from '@/components/FullscreenPreview.vue'

const fileList = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)

const filters = reactive({
  fileType: '', materialType: '', relatedType: '', keyword: ''
})
const dateRange = ref([])

const stats = reactive({
  totalFiles: 0, totalStorage: 0, recentUploads: 0, countByType: {}
})

const uploadVisible = ref(false)
const uploadMaterialType = ref('')
const uploadAreaConfig = {
  accept: '.jpg,.jpeg,.png,.gif,.bmp,.webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar,.7z,.tar,.gz,.mp4,.avi,.mov,.wmv,.flv',
  maxSize: 10 * 1024 * 1024,
  maxCount: 5
}

const selectedIds = ref([])
const previewVisible = ref(false)
const previewFiles = ref([])
const previewIndex = ref(0)
const previewContextLabel = ref('')

const materialTypes = [
  '获奖证书', '现场合影', '其他材料',
  '立项申报书', '结题材料', '结题证书',
  '投稿初稿', '录用终稿', '审稿意见',
  '申报材料', '证书扫描件'
]

onMounted(() => {
  fetchData()
  fetchStats()
})

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.fileType) params.fileType = filters.fileType
    if (filters.materialType) params.materialType = filters.materialType
    if (filters.relatedType) params.relatedType = filters.relatedType
    if (filters.keyword) params.keyword = filters.keyword
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getFileList(params)
    if (res.code === 200) {
      fileList.value = res.data.content
      total.value = res.data.totalElements
    }
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    const res = await getFileStats()
    if (res.code === 200) {
      Object.assign(stats, res.data)
    }
  } catch { /* ignore */ }
}

function handleQuery() {
  page.value = 1
  fetchData()
}

function handleReset() {
  filters.fileType = ''
  filters.materialType = ''
  filters.relatedType = ''
  filters.keyword = ''
  dateRange.value = []
  page.value = 1
  fetchData()
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

function handlePreview(row) {
  previewFiles.value = [{
    id: row.id,
    url: getFileUrl(row.id),
    originalName: row.originalName,
    fileType: row.fileType
  }]
  previewIndex.value = 0
  previewContextLabel.value = ''
  previewVisible.value = true
}

async function handleDownload(row) {
  try {
    const res = await downloadSingleFile(row.id)
    downloadBlob(res, row.originalName)
  } catch {
    ElMessage.error('下载失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除文件 "${row.originalName}"？`, '确认删除', { type: 'warning' })
    const res = await deleteFile(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
      fetchStats()
    }
  } catch { /* cancel */ }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个文件？`, '批量删除', { type: 'warning' })
    for (const id of selectedIds.value) {
      await deleteFile(id)
    }
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    fetchData()
    fetchStats()
  } catch { /* cancel */ }
}

function onUploadSuccess() {
  ElMessage.success('上传成功')
  uploadVisible.value = false
  fetchData()
  fetchStats()
}

function fileTypeTag(type) {
  const map = { IMAGE: '', DOCUMENT: 'success', ARCHIVE: 'warning', VIDEO: 'danger', OTHER: 'info' }
  return map[type] || 'info'
}

function fileTypeLabel(type) {
  const map = { IMAGE: '图片', DOCUMENT: '文档', ARCHIVE: '压缩包', VIDEO: '视频', OTHER: '其他' }
  return map[type] || type
}

function formatFileSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  const val = (bytes / Math.pow(k, i)).toFixed(i === 0 ? 0 : 1)
  return val + ' ' + units[i]
}

function formatTime(time) {
  if (!time) return '-'
  return time.substring(0, 19).replace('T', ' ')
}
</script>

<style scoped>
.stat-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--card-radius);
  padding: var(--space-md) var(--space-lg);
}
.stat-card-value {
  font-size: var(--text-3xl);
  font-weight: 700;
  color: var(--text-primary);
  line-height: var(--leading-tight);
  letter-spacing: -0.02em;
}
.stat-card-label {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  margin-top: 4px;
  font-weight: 500;
}
.stat-card-tags {
  min-height: 28px;
}
</style>
