<template>
  <div class="file-upload-wrapper">
    <div class="upload-hint-text">{{ areaConfig?.hint || hint }}</div>
    <el-upload
      ref="uploadRef"
      :auto-upload="true"
      :action="uploadUrl"
      :headers="headers"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-remove="handleRemove"
      :before-upload="beforeUpload"
      :file-list="displayList"
      :limit="maxCount"
      :on-exceed="handleExceed"
      list-type="picture-card"
      drag
      multiple
    >
      <el-icon><Plus /></el-icon>
      <div class="el-upload__text">拖拽文件到此处或 <em>点击上传</em></div>
    </el-upload>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const props = defineProps({
  existingFileIds: { type: Array, default: () => [] },
  hint: { type: String, default: '支持 JPG/PNG/PDF/DOC/DOCX/ZIP 格式，单个文件不超过 10MB' },
  /** 材料类型标签（用于文件命名） */
  materialType: { type: String, default: '' },
  /** 上传区域配置（来自 uploadConfig） */
  areaConfig: { type: Object, default: null }
})

const emit = defineEmits(['update:fileIds', 'file-uploaded'])

const baseURL = request.defaults.baseURL
const uploadRef = ref(null)

const maxCount = computed(() => props.areaConfig?.maxCount || 99)

const uploadUrl = computed(() => {
  let url = `${baseURL}/files/upload`
  if (props.materialType) {
    url += `?materialType=${encodeURIComponent(props.materialType)}`
  }
  return url
})

const headers = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`
}))

const displayList = ref([])
const allIds = ref([...props.existingFileIds])

onMounted(() => {
  for (const id of props.existingFileIds) {
    displayList.value.push({
      name: `文件 ${id}`,
      url: `${baseURL}/files/${id}`,
      uid: -id,
      isExisting: true,
      fileId: id
    })
  }
  emit('update:fileIds', [...allIds.value])
})

function beforeUpload(file) {
  const config = props.areaConfig
  const maxSize = config?.maxSize || 10

  // 大小检查
  const maxBytes = maxSize * 1024 * 1024
  if (file.size > maxBytes) {
    ElMessage.error(`文件大小不能超过 ${maxSize}MB`)
    return false
  }

  // 类型检查（优先用 areaConfig，其次用默认列表）
  if (config?.accept && config.acceptExt) {
    const allowedTypes = config.accept.split(',').map(s => s.trim().toLowerCase())
    const ext = '.' + ((file.name.split('.').pop() || '').toLowerCase())
    const allowedExts = config.acceptExt.split(',').map(s => s.trim().toLowerCase())

    if (!allowedTypes.includes(file.type.toLowerCase()) && !allowedExts.includes(ext)) {
      ElMessage.error(`不支持的文件格式，支持：${config.acceptExt}`)
      return false
    }
  }

  return true
}

function handleExceed() {
  ElMessage.warning(`最多上传 ${maxCount.value} 个文件`)
}

function handleSuccess(response, file) {
  if (response.code === 200 && response.data) {
    const fileInfo = {
      id: response.data.id,
      originalName: response.data.originalName || file.name,
      fileType: response.data.fileType || 'OTHER',
      materialType: props.materialType
    }
    allIds.value.push(response.data.id)
    emit('update:fileIds', [...allIds.value])
    emit('file-uploaded', fileInfo)

    // 按上传区域的 materialType 重命名显示
    if (props.materialType) {
      const dotIdx = file.name.lastIndexOf('.')
      const ext = dotIdx >= 0 ? file.name.substring(dotIdx) : ''
      const newName = props.materialType + ext
      file.name = newName
      const uploadFiles = uploadRef.value?.uploadFiles || []
      const match = uploadFiles.find(f => f.uid === file.uid)
      if (match) match.name = newName
    }

    ElMessage.success(`${file.name} 上传成功`)
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleError() {
  ElMessage.error('上传失败，请重试')
}

function handleRemove(file) {
  if (file.isExisting && file.fileId) {
    const idx = allIds.value.indexOf(file.fileId)
    if (idx > -1) {
      allIds.value.splice(idx, 1)
      emit('update:fileIds', [...allIds.value])
    }
    return
  }
  if (file.response?.data?.id !== undefined) {
    const idx = allIds.value.indexOf(file.response.data.id)
    if (idx > -1) {
      allIds.value.splice(idx, 1)
      emit('update:fileIds', [...allIds.value])
    }
  }
}
</script>

<style scoped>
.file-upload-wrapper { width: 100%; }

.upload-hint-text {
  display: block;
  width: 100%;
  margin-bottom: 10px;
  padding: 6px 10px;
  font-size: 13px;
  color: #6c757d;
  background: #f8f9fa;
  border-left: 3px solid var(--primary-color);
  border-radius: 4px;
  line-height: 1.7;
  white-space: normal;
  word-break: break-word;
  overflow: visible;
  text-align: left;
}
</style>
