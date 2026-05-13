<template>
  <div class="file-upload-wrapper">
    <div class="upload-hint-text">{{ hint }}</div>
    <el-upload
      ref="uploadRef"
      :auto-upload="true"
      :action="`${baseURL}/files/upload`"
      :headers="headers"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-remove="handleRemove"
      :before-upload="beforeUpload"
      :file-list="displayList"
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
  hint: { type: String, default: '支持 JPG/PNG/PDF/DOC/DOCX/ZIP 格式，单个文件不超过 10MB' }
})

const emit = defineEmits(['update:fileIds'])
const baseURL = request.defaults.baseURL
const uploadRef = ref(null)

const headers = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`
}))

const displayList = ref([])
const allIds = ref([...props.existingFileIds])

onMounted(() => {
  // Build display list for existing files
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
  const allowedTypes = [
    'image/png', 'image/jpeg', 'image/gif', 'image/bmp', 'image/webp',
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/zip', 'application/x-zip-compressed',
    'video/mp4', 'video/quicktime', 'video/x-msvideo'
  ]
  const allowedExts = /\.(png|jpg|jpeg|gif|bmp|webp|pdf|doc|docx|zip|mp4)$/i
  const isAllowedType = allowedTypes.includes(file.type)
  const isAllowedExt = allowedExts.test(file.name)
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isAllowedType && !isAllowedExt) {
    ElMessage.error('不支持的文件格式，支持 JPG/PNG/PDF/DOC/DOCX/ZIP/MP4')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

function handleSuccess(response, file) {
  if (response.code === 200 && response.data) {
    allIds.value.push(response.data.id)
    emit('update:fileIds', [...allIds.value])
    ElMessage.success(`${file.name} 上传成功`)
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleError() {
  ElMessage.error('上传失败，请重试')
}

function handleRemove(file) {
  // Remove existing file
  if (file.isExisting && file.fileId) {
    const idx = allIds.value.indexOf(file.fileId)
    if (idx > -1) {
      allIds.value.splice(idx, 1)
      emit('update:fileIds', [...allIds.value])
    }
    return
  }
  // Remove newly uploaded file
  if (file.response && file.response.data && file.response.data.id !== undefined) {
    const idx = allIds.value.indexOf(file.response.data.id)
    if (idx > -1) {
      allIds.value.splice(idx, 1)
      emit('update:fileIds', [...allIds.value])
    }
  }
}
</script>

<style scoped>
.file-upload-wrapper {
  width: 100%;
}

.upload-hint-text {
  display: block;
  width: 100%;
  margin-bottom: 10px;
  padding: 6px 10px;
  font-size: 13px;
  color: #6c757d;
  background: #f8f9fa;
  border-left: 3px solid var(--primary-color, #1883E5);
  border-radius: 4px;
  line-height: 1.7;
  white-space: normal;
  word-break: break-word;
  overflow: visible;
  text-align: left;
}
</style>
