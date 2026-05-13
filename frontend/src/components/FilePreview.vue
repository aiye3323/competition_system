<template>
  <el-button type="primary" link @click="dialogVisible = true">
    {{ btnText }}
  </el-button>

  <el-dialog v-model="dialogVisible" :title="fileName" width="65%" top="3vh">
    <!-- 图片预览 -->
    <div v-if="isImage" style="text-align:center;">
      <img :src="fileUrl" style="max-width:100%; max-height:75vh;" />
    </div>
    <!-- PDF预览 -->
    <div v-else-if="isPdf" style="height:75vh;">
      <iframe :src="fileUrl" width="100%" height="100%" style="border:none;"></iframe>
    </div>
    <!-- 视频预览 -->
    <div v-else-if="isVideo" style="text-align:center;">
      <video :src="fileUrl" controls style="max-width:100%; max-height:75vh;">
        您的浏览器不支持视频播放
      </video>
    </div>
    <!-- 不可预览 -->
    <div v-else style="text-align:center; padding:40px;">
      <el-icon :size="48" color="#909399"><Document /></el-icon>
      <p class="text-regular" style="margin:16px 0;">
        {{ previewHint }}
      </p>
      <el-button type="primary" @click="handleDownload">
        <el-icon><Download /></el-icon> 下载文件
      </el-button>
    </div>
    <template #footer>
      <el-button @click="handleDownload">
        <el-icon><Download /></el-icon> 下载
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Document, Download } from '@element-plus/icons-vue'
import { getFileUrl } from '@/api/file'

const props = defineProps({
  fileId: { type: [Number, String], required: true },
  fileName: { type: String, default: '' },
  fileType: { type: String, default: '' }
})

const dialogVisible = ref(false)
const btnText = computed(() => {
  if (props.fileType === 'IMAGE' || isPdf.value || isVideo.value) return '预览'
  return '查看'
})
const fileUrl = computed(() => getFileUrl(props.fileId))
const isImage = computed(() => props.fileType === 'IMAGE')
const isPdf = computed(() => {
  const name = (props.fileName || '').toLowerCase()
  return props.fileType === 'DOCUMENT' && name.endsWith('.pdf')
})
const isVideo = computed(() => {
  const name = (props.fileName || '').toLowerCase()
  return props.fileType === 'VIDEO' || /\.(mp4|webm|ogg)$/i.test(name)
})

const previewHint = computed(() => {
  const name = (props.fileName || '').toLowerCase()
  if (/\.(doc|docx)$/i.test(name)) return 'Word 文档暂不支持在线预览，请下载后查看'
  if (/\.(zip|rar|7z)$/i.test(name)) return '压缩文件暂不支持在线预览，请下载后解压查看'
  return '此文件类型暂不支持在线预览，请下载后查看'
})

function handleDownload() {
  window.open(fileUrl.value, '_blank')
}
</script>
