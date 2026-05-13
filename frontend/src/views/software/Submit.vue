<template>
  <div class="page-container">
    <h2>{{ isEdit ? '编辑软件著作权' : '提交软件著作权' }}</h2>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="软件名称" prop="softwareName">
        <el-input v-model="form.softwareName" placeholder="请输入软件名称" />
      </el-form-item>
      <el-form-item label="所属单位" prop="affiliation">
        <el-select v-model="form.affiliation" placeholder="请选择" style="width:100%">
          <el-option label="怀化学院" value="怀化学院" />
          <el-option label="其他单位" value="其他单位" />
        </el-select>
      </el-form-item>
      <el-form-item label="著作权人" prop="copyrightOwner">
        <el-input v-model="form.copyrightOwner" placeholder="请输入著作权人" />
      </el-form-item>
      <el-form-item label="登记号" prop="registrationNumber">
        <el-input v-model="form.registrationNumber" placeholder="如 2025SR11569875" />
      </el-form-item>
      <el-form-item label="登记日期" prop="registrationDate">
        <el-date-picker v-model="form.registrationDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="证明材料">
        <FileUpload :existing-file-ids="existingFileIds" hint="支持 ZIP、PNG、PDF 格式，ZIP 不超过20MB，其余不超过10MB" @update:file-ids="fileIds = $event" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit">提交</el-button>
        <el-button @click="$router.push('/software/list')">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitSoftware, getSoftwareDetail, updateSoftware } from '@/api/software'
import FileUpload from '@/components/FileUpload.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const fileIds = ref([])
const existingFileIds = ref([])
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  softwareName: '',
  affiliation: '',
  copyrightOwner: '',
  registrationNumber: '',
  registrationDate: ''
})

const rules = {
  softwareName: [{ required: true, message: '请输入软件名称', trigger: 'blur' }],
  affiliation: [{ required: true, message: '请选择所属单位', trigger: 'change' }],
  registrationNumber: [{ required: true, message: '请输入登记号', trigger: 'blur' }]
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await getSoftwareDetail(route.params.id)
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
      router.push('/software/list')
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
      ? await updateSoftware(route.params.id, data)
      : await submitSoftware(data)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '提交成功')
      router.push('/software/list')
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } finally {
    loading.value = false
  }
}
</script>
