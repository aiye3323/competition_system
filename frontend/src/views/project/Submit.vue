<template>
  <div class="page-container">
    <h2>{{ isEdit ? '编辑创新项目' : '提交创新项目' }}</h2>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="项目名称" prop="projectName">
        <el-input v-model="form.projectName" placeholder="请输入项目名称" />
      </el-form-item>
      <el-form-item label="立项级别" prop="projectLevel">
        <el-select v-model="form.projectLevel" placeholder="请选择" style="width:100%">
          <el-option label="国家级" value="国家级" />
          <el-option label="省级" value="省级" />
          <el-option label="校级" value="校级" />
          <el-option label="院级" value="院级" />
        </el-select>
      </el-form-item>
      <el-form-item label="立项类型" prop="projectType">
        <el-select v-model="form.projectType" placeholder="请选择" style="width:100%">
          <el-option label="创新训练" value="创新训练" />
          <el-option label="创业训练" value="创业训练" />
          <el-option label="创业实践" value="创业实践" />
        </el-select>
      </el-form-item>
      <el-form-item label="指导教师" prop="advisor">
        <el-input v-model="form.advisor" placeholder="多位教师用逗号分隔" />
      </el-form-item>
      <el-form-item label="立项人员" prop="members">
        <el-input v-model="form.members" type="textarea" :rows="2" placeholder="多人用逗号分隔" />
      </el-form-item>
      <el-form-item label="立项时间" prop="establishTime">
        <el-date-picker v-model="form.establishTime" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="附件材料">
        <FileUpload :existing-file-ids="existingFileIds" hint="支持 DOC、DOCX、PDF、JPG、PNG、ZIP 格式，单个文件不超过10MB" @update:file-ids="fileIds = $event" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit">{{ isEdit ? '保存修改' : '提交' }}</el-button>
        <el-button @click="$router.push('/project/list')">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitProject, getProjectDetail, updateProject } from '@/api/project'
import FileUpload from '@/components/FileUpload.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const fileIds = ref([])
const existingFileIds = ref([])
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  projectName: '',
  projectLevel: '',
  projectType: '',
  advisor: '',
  members: '',
  establishTime: ''
})

const rules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectLevel: [{ required: true, message: '请选择立项级别', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择立项类型', trigger: 'change' }]
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await getProjectDetail(route.params.id)
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
      router.push('/project/list')
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
      ? await updateProject(route.params.id, data)
      : await submitProject(data)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '提交成功')
      router.push('/project/list')
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } finally {
    loading.value = false
  }
}
</script>
