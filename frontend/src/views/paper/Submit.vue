<template>
  <div class="submit-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑论文成果' : '提交论文成果' }}</h2>
      <p class="header-desc">填写论文信息并上传投稿初稿、录用终稿等证明材料</p>
    </div>

    <el-card class="submit-card">
      <div class="form-section">
        <h3 class="section-title">基本信息</h3>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
          <el-form-item label="论文标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入论文标题" />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="期刊/会议名称" prop="journalName">
                <el-input v-model="form.journalName" placeholder="期刊或会议名称" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="期刊级别" prop="journalLevel">
                <el-select v-model="form.journalLevel" placeholder="请选择">
                  <el-option label="SCI一区" value="SCI一区" /><el-option label="SCI二区" value="SCI二区" />
                  <el-option label="SCI三区" value="SCI三区" /><el-option label="SCI四区" value="SCI四区" />
                  <el-option label="EI期刊" value="EI期刊" />
                  <el-option label="北大核心期刊" value="北大核心期刊" /><el-option label="省级期刊" value="省级期刊" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="投稿日期" prop="submissionDate">
                <el-date-picker v-model="form.submissionDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="关键词" prop="keywords">
                <el-input v-model="form.keywords" placeholder="多个用逗号分隔" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="录用日期" prop="acceptanceDate">
                <el-date-picker v-model="form.acceptanceDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="作者列表" prop="authors">
            <el-input v-model="form.authors" type="textarea" :rows="2" placeholder="多人用逗号分隔" />
          </el-form-item>
        </el-form>
      </div>

      <el-divider />

      <div class="form-section">
        <h3 class="section-title">证明材料</h3>
        <el-form label-width="110px">
          <el-form-item label="投稿初稿" required>
            <FileUpload :area-config="paperAreas[0]" :existing-file-ids="draftFiles" material-type="投稿初稿" @update:file-ids="draftIds = $event" />
          </el-form-item>
          <el-form-item label="录用终稿" required>
            <div style="display:flex;align-items:flex-start;gap:8px;width:100%;">
              <FileUpload :area-config="paperAreas[1]" :existing-file-ids="finalFiles" material-type="录用终稿" @update:file-ids="finalIds = $event" style="flex:1;" />
              <el-button size="small" :loading="ocrLoading" @click="handleOcr('paper')" :disabled="finalIds.length === 0">识别填充</el-button>
            </div>
          </el-form-item>
          <el-form-item label="审稿意见" required>
            <FileUpload :area-config="paperAreas[2]" :existing-file-ids="reviewFiles" material-type="审稿意见及回复" @update:file-ids="reviewIds = $event" />
          </el-form-item>
        </el-form>
      </div>

      <div class="form-actions">
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '提交' }}
        </el-button>
        <el-button @click="$router.push('/paper/list')">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitPaper, getPaperDetail, updatePaper } from '@/api/paper'
import FileUpload from '@/components/FileUpload.vue'
import { paperAreas } from '@/utils/uploadConfig'
import { recognizeOcrById } from '@/api/ocr'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const ocrLoading = ref(false)
const draftIds = ref([])
const finalIds = ref([])
const reviewIds = ref([])
const draftFiles = ref([])
const finalFiles = ref([])
const reviewFiles = ref([])
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  title: '', journalName: '', journalLevel: '', keywords: '',
  authors: '', submissionDate: '', acceptanceDate: ''
})

const rules = {
  title: [{ required: true, message: '请输入论文标题', trigger: 'blur' }],
  journalName: [{ required: true, message: '请输入期刊/会议名称', trigger: 'blur' }],
  journalLevel: [{ required: true, message: '请选择期刊级别', trigger: 'change' }],
  keywords: [{ required: true, message: '请输入关键词', trigger: 'blur' }],
  authors: [{ required: true, message: '请输入作者列表', trigger: 'blur' }],
  submissionDate: [{ required: true, message: '请选择投稿日期', trigger: 'change' }],
  acceptanceDate: [{ required: true, message: '请选择录用日期', trigger: 'change' }]
}

function fillIfEmpty(f, v) { return (!f && v) ? v : f }

async function handleOcr(type) {
  const fileId = finalIds.value[0]
  if (!fileId) { ElMessage.warning('请先上传录用终稿再点击识别'); return }
  ocrLoading.value = true
  try {
    const res = await recognizeOcrById(fileId, type)
    if (res.code === 200 && res.data?.success) {
      const d = res.data
      form.title = fillIfEmpty(form.title, d.paperTitle || d.name)
      form.journalName = fillIfEmpty(form.journalName, d.journalName)
      form.journalLevel = fillIfEmpty(form.journalLevel, d.journalLevel)
      if (!form.acceptanceDate && d.acceptanceDate) form.acceptanceDate = d.acceptanceDate
      ElMessage.success('识别完成，已自动填写表单，请核对修改')
    } else { ElMessage.warning('识别失败：' + (res.data?.errorMessage || '请手动填写')) }
  } catch { ElMessage.error('识别服务异常，请手动填写') }
  finally { ocrLoading.value = false }
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await getPaperDetail(route.params.id)
      if (res.code === 200) {
        Object.assign(form, res.data)
        if (res.data.fileUrlList) {
          const byMaterial = groupByMaterial(res.data.fileUrlList)
          draftFiles.value = byMaterial['投稿初稿'] || []
          finalFiles.value = byMaterial['录用终稿'] || []
          reviewFiles.value = byMaterial['审稿意见及回复'] || []
          draftIds.value = draftFiles.value.map(f => f.id)
          finalIds.value = finalFiles.value.map(f => f.id)
          reviewIds.value = reviewFiles.value.map(f => f.id)
        }
      }
    } catch {
      ElMessage.error('加载数据失败')
      router.push('/paper/list')
    }
  }
})

function groupByMaterial(list) {
  const map = {}
  for (const f of list) {
    const key = f.materialType || '其他'
    if (!map[key]) map[key] = []
    map[key].push(f)
  }
  return map
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (draftIds.value.length === 0) { ElMessage.error('请上传投稿初稿'); return }
  if (finalIds.value.length === 0) { ElMessage.error('请上传录用终稿'); return }
  if (reviewIds.value.length === 0) { ElMessage.error('请上传审稿意见及回复'); return }

  loading.value = true
  try {
    const fileIds = [...draftIds.value, ...finalIds.value, ...reviewIds.value]
    const data = { ...form, fileIds }
    const res = isEdit.value
      ? await updatePaper(route.params.id, data)
      : await submitPaper(data)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '提交成功')
      router.push('/paper/list')
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } finally { loading.value = false }
}
</script>

<style scoped>
.submit-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header { margin-bottom: var(--space-lg); }
.page-header h2 { font-size: var(--text-xl); font-weight: 600; color: var(--text-primary); margin: 0 0 4px; }
.header-desc { font-size: var(--text-sm); color: var(--text-secondary); margin: 0; }

.submit-card { border-radius: 12px; }

:deep(.el-card__body) { padding: 32px 40px; }

.form-section { margin-bottom: var(--space-lg); }
.section-title { font-size: var(--text-base); font-weight: 600; color: var(--text-primary); margin: 0 0 var(--space-lg); }

:deep(.el-form-item) { margin-bottom: 22px; }
:deep(.el-form-item__label) { font-weight: 500; }
:deep(.el-input__wrapper) { border-radius: 6px; }
:deep(.el-select) { width: 100%; }
:deep(.el-upload-dragger) { width: 100%; border-radius: 8px; }

.form-actions {
  display: flex;
  justify-content: center;
  gap: var(--space-lg);
  margin-top: var(--space-xl);
  padding-bottom: var(--space-sm);
}

.form-actions .el-button {
  padding: 10px 32px;
  font-size: var(--text-sm);
  border-radius: 6px;
  min-width: 100px;
}

@media (max-width: 768px) {
  .submit-container { padding: 12px; }
  :deep(.el-card__body) { padding: 20px; }
}
</style>
