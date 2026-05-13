<template>
  <el-dialog v-model="dialogVisible" :title="title" width="500px" :close-on-click-modal="false">
    <el-form label-width="80px">
      <el-form-item label="审核意见">
        <el-input v-model="opinion" type="textarea" :rows="4" :placeholder="rejectPlaceholder" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="danger" @click="submitAudit('reject')" :loading="loading">退回</el-button>
      <el-button type="primary" @click="submitAudit('approve')" :loading="loading">通过</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  title: { type: String, default: '审核' },
  approveFn: { type: Function, required: true },
  rejectFn: { type: Function, required: true },
  rejectPlaceholder: { type: String, default: '退回时必须填写审核意见' }
})

const emit = defineEmits(['update:visible', 'success'])

const dialogVisible = computed({
  get: () => props.visible,
  set: v => emit('update:visible', v)
})

const opinion = ref('')
const loading = ref(false)

async function submitAudit(action) {
  if (action === 'reject' && !opinion.value.trim()) {
    ElMessage.warning(props.rejectPlaceholder)
    return
  }
  loading.value = true
  try {
    const fn = action === 'approve' ? props.approveFn : props.rejectFn
    const res = await fn({ opinion: opinion.value })
    if (res.code === 200) {
      ElMessage.success(action === 'approve' ? '审核通过' : '已退回')
      dialogVisible.value = false
      opinion.value = ''
      emit('success')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    loading.value = false
  }
}
</script>
