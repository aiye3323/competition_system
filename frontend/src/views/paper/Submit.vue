<template>
  <div class="page-container">
    <h2>{{ isEdit ? '编辑论文成果' : '提交论文成果' }}</h2>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="论文标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入论文标题" />
      </el-form-item>
      <el-form-item label="期刊/会议名称" prop="journalName">
        <el-input v-model="form.journalName" placeholder="请输入期刊或会议名称" />
      </el-form-item>
      <el-form-item label="期刊级别" prop="journalLevel">
        <el-select v-model="form.journalLevel" placeholder="请选择" style="width:100%">
          <el-option label="SCI一区" value="SCI一区" />
          <el-option label="SCI二区" value="SCI二区" />
          <el-option label="SCI三区" value="SCI三区" />
          <el-option label="SCI四区" value="SCI四区" />
          <el-option label="EI期刊" value="EI期刊" />
          <el-option label="CCF A类会议" value="CCF A类会议" />
          <el-option label="CCF B类会议" value="CCF B类会议" />
          <el-option label="CCF C类会议" value="CCF C类会议" />
          <el-option label="EI会议" value="EI会议" />
          <el-option label="北大核心期刊" value="北大核心期刊" />
          <el-option label="省级期刊" value="省级期刊" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="keywords">
        <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔" />
      </el-form-item>
      <el-form-item label="作者列表" prop="authors">
        <el-input v-model="form.authors" type="textarea" :rows="2" placeholder="多人用逗号分隔" />
      </el-form-item>
      <el-form-item label="投稿日期" prop="submissionDate">
        <el-date-picker v-model="form.submissionDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="录用日期" prop="acceptanceDate">
        <el-date-picker v-model="form.acceptanceDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="附件材料">
        <FileUpload :existing-file-ids="existingFileIds" hint="支持 PDF 格式，单个文件不超过10MB" @update:file-ids="fileIds = $event" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit">{{ isEdit ? '保存修改' : '提交' }}</el-button>
        <el-button @click="$router.push('/paper/list')">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitPaper, getPaperDetail, updatePaper } from '@/api/paper'
import FileUpload from '@/components/FileUpload.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const fileIds = ref([])
const existingFileIds = ref([])
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  title: '',
  journalName: '',
  journalLevel: '',
  keywords: '',
  authors: '',
  submissionDate: '',
  acceptanceDate: ''
})

const rules = {
  title: [{ required: true, message: '请输入论文标题', trigger: 'blur' }],
  journalLevel: [{ required: true, message: '请选择期刊级别', trigger: 'change' }]
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await getPaperDetail(route.params.id)
      if (res.code === 200) {
        Object.assign(form, res.data)
        if (res.data.fileUrlList && res.data.fileUrlList.length > 0) {
          existingFileIds.value = res.data.fileUrlList
            .map(f => f.id)
            .filter(id => id !== null)
          fileIds.value = [...existingFileIds.value]
        }
      }
    } catch (e) {
      ElMessage.error('加载数据失败')
      router.push('/paper/list')
    }
  }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const data = { ...form, fileIds: fileIds.value }
    const res = isEdit.value
      ? await updatePaper(route.params.id, data)
      : await submitPaper(data)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '提交成功')
      router.push('/paper/list')
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } finally {
    loading.value = false
  }
}
</script>
