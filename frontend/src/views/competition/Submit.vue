<template>
  <div class="page-container">
    <h2>{{ isEdit ? '编辑学科竞赛成果' : '提交学科竞赛成果' }}</h2>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="竞赛类别" prop="category">
        <el-select v-model="form.category" placeholder="请选择" style="width:100%">
          <el-option label="A类" value="A类" />
          <el-option label="B类" value="B类" />
          <el-option label="C类" value="C类" />
        </el-select>
      </el-form-item>
      <el-form-item label="竞赛名称" prop="competitionName">
        <el-input v-model="form.competitionName" placeholder="请输入竞赛名称" />
      </el-form-item>
      <el-form-item label="获奖级别" prop="awardLevel">
        <el-select v-model="form.awardLevel" placeholder="请选择" style="width:100%">
          <el-option label="国家级" value="国家级" />
          <el-option label="省级" value="省级" />
          <el-option label="市级" value="市级" />
          <el-option label="校级" value="校级" />
          <el-option label="院级" value="院级" />
        </el-select>
      </el-form-item>
      <el-form-item label="获奖等级" prop="awardGrade">
        <el-select v-model="form.awardGrade" placeholder="请选择" style="width:100%">
          <el-option label="一等奖" value="一等奖" />
          <el-option label="二等奖" value="二等奖" />
          <el-option label="三等奖" value="三等奖" />
          <el-option label="优秀奖" value="优秀奖" />
        </el-select>
      </el-form-item>
      <el-form-item label="获奖单位" prop="awardUnit">
        <el-input v-model="form.awardUnit" placeholder="请输入颁奖单位" />
      </el-form-item>
      <el-form-item label="主办单位" prop="organizer">
        <el-input v-model="form.organizer" placeholder="请输入主办单位" />
      </el-form-item>
      <el-form-item label="承办单位" prop="coOrganizer">
        <el-input v-model="form.coOrganizer" placeholder="请输入承办单位" />
      </el-form-item>
      <el-form-item label="获奖时间" prop="awardDate">
        <el-date-picker v-model="form.awardDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="指导教师" prop="advisor">
        <el-input v-model="form.advisor" placeholder="多位教师用逗号分隔" />
      </el-form-item>
      <el-form-item label="参赛选手" prop="participants">
        <el-input v-model="form.participants" type="textarea" :rows="2" placeholder="多人用逗号分隔" />
      </el-form-item>
      <el-form-item label="附件材料">
        <FileUpload :existing-file-ids="existingFileIds" hint="支持 JPG、PNG、PDF、ZIP 格式，单个文件不超过10MB" @update:file-ids="fileIds = $event" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit">{{ isEdit ? '保存修改' : '提交' }}</el-button>
        <el-button @click="$router.push('/competition/list')">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitCompetition, updateCompetition, getCompetitionDetail } from '@/api/competition'
import FileUpload from '@/components/FileUpload.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const fileIds = ref([])
const existingFileIds = ref([])
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  category: '',
  competitionName: '',
  awardLevel: '',
  awardGrade: '',
  awardUnit: '',
  organizer: '',
  coOrganizer: '',
  awardDate: '',
  advisor: '',
  participants: ''
})

const rules = {
  category: [{ required: true, message: '请选择竞赛类别', trigger: 'change' }],
  competitionName: [{ required: true, message: '请输入竞赛名称', trigger: 'blur' }],
  awardLevel: [{ required: true, message: '请选择获奖级别', trigger: 'change' }],
  awardGrade: [{ required: true, message: '请选择获奖等级', trigger: 'change' }]
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await getCompetitionDetail(route.params.id)
      if (res.code === 200) {
        Object.assign(form, res.data)
        // Extract file IDs from fileUrlList (FileInfo objects)
        if (res.data.fileUrlList && res.data.fileUrlList.length > 0) {
          existingFileIds.value = res.data.fileUrlList
            .map(f => f.id)
            .filter(id => id !== null)
          fileIds.value = [...existingFileIds.value]
        }
      }
    } catch (e) {
      ElMessage.error('加载数据失败')
      router.push('/competition/list')
    }
  }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = { ...form, fileIds: fileIds.value }
    let res
    if (isEdit.value) {
      res = await updateCompetition(route.params.id, data)
    } else {
      res = await submitCompetition(data)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '提交成功')
      router.push('/competition/list')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    loading.value = false
  }
}
</script>
