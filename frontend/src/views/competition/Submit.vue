<template>
  <div class="submit-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑学科竞赛成果' : '提交学科竞赛成果' }}</h2>
      <p class="header-desc">填写竞赛信息并上传获奖证书等证明材料</p>
    </div>

    <el-card class="submit-card">
      <!-- 基本信息 -->
      <div class="form-section">
        <h3 class="section-title">基本信息</h3>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="竞赛类别" prop="category">
                <el-select v-model="form.category" placeholder="请选择">
                  <el-option label="A类" value="A类" /><el-option label="B类" value="B类" /><el-option label="C类" value="C类" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="获奖级别" prop="awardLevel">
                <el-select v-model="form.awardLevel" placeholder="请选择">
                  <el-option label="国家级" value="国家级" /><el-option label="省级" value="省级" />
                  <el-option label="市级" value="市级" /><el-option label="校级" value="校级" /><el-option label="院级" value="院级" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="获奖等级" prop="awardGrade">
                <el-select v-model="form.awardGrade" placeholder="请选择">
                  <el-option label="一等奖" value="一等奖" /><el-option label="二等奖" value="二等奖" />
                  <el-option label="三等奖" value="三等奖" /><el-option label="优秀奖" value="优秀奖" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="竞赛名称" prop="competitionName">
            <el-input v-model="form.competitionName" placeholder="请输入竞赛名称" />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="获奖单位" prop="awardUnit">
                <el-input v-model="form.awardUnit" placeholder="颁奖单位" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="主办单位" prop="organizer">
                <el-input v-model="form.organizer" placeholder="主办单位" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="承办单位" prop="coOrganizer">
                <el-input v-model="form.coOrganizer" placeholder="承办单位" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="指导教师" prop="advisor">
                <el-input v-model="form.advisor" placeholder="多位用逗号分隔" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="获奖时间" prop="awardDate">
                <el-date-picker v-model="form.awardDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="参赛选手" prop="participants">
            <el-input v-model="form.participants" type="textarea" :rows="2" placeholder="多人用逗号分隔" />
          </el-form-item>
        </el-form>
      </div>

      <el-divider />

      <!-- 证明材料 -->
      <div class="form-section">
        <h3 class="section-title">证明材料</h3>
        <el-form label-width="100px">
          <el-form-item label="获奖证书" required>
            <div style="display:flex;align-items:flex-start;gap:8px;width:100%;">
              <FileUpload :area-config="competitionAreas[0]" :existing-file-ids="certFiles" material-type="获奖证书" @update:file-ids="certIds = $event" style="flex:1;" />
              <el-button size="small" :loading="ocrLoading" @click="handleOcr('competition')" :disabled="certIds.length === 0">识别填充</el-button>
            </div>
          </el-form-item>
          <el-form-item label="现场合影">
            <FileUpload :area-config="competitionAreas[1]" :existing-file-ids="photoFiles" material-type="现场合影" @update:file-ids="photoIds = $event" />
          </el-form-item>
          <el-form-item label="其他材料">
            <FileUpload :area-config="competitionAreas[2]" :existing-file-ids="otherFiles" material-type="其他材料" @update:file-ids="otherIds = $event" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 操作 -->
      <div class="form-actions">
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '提交' }}
        </el-button>
        <el-button @click="$router.push('/competition/list')">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitCompetition, updateCompetition, getCompetitionDetail } from '@/api/competition'
import FileUpload from '@/components/FileUpload.vue'
import { competitionAreas } from '@/utils/uploadConfig'
import { recognizeOcrById } from '@/api/ocr'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const ocrLoading = ref(false)
const certIds = ref([])
const photoIds = ref([])
const otherIds = ref([])
const certFiles = ref([])
const photoFiles = ref([])
const otherFiles = ref([])
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  category: '', competitionName: '', awardLevel: '', awardGrade: '',
  awardUnit: '', organizer: '', coOrganizer: '', awardDate: '',
  advisor: '', participants: ''
})

const rules = {
  category: [{ required: true, message: '请选择竞赛类别', trigger: 'change' }],
  competitionName: [{ required: true, message: '请输入竞赛名称', trigger: 'blur' }],
  awardLevel: [{ required: true, message: '请选择获奖级别', trigger: 'change' }],
  awardGrade: [{ required: true, message: '请选择获奖等级', trigger: 'change' }],
  awardUnit: [{ required: true, message: '请输入获奖单位', trigger: 'blur' }],
  organizer: [{ required: true, message: '请输入主办单位', trigger: 'blur' }],
  coOrganizer: [{ required: true, message: '请输入承办单位', trigger: 'blur' }],
  awardDate: [{ required: true, message: '请选择获奖时间', trigger: 'change' }],
  advisor: [{ required: true, message: '请输入指导教师', trigger: 'blur' }],
  participants: [{ required: true, message: '请输入参赛选手', trigger: 'blur' }]
}

function fillIfEmpty(field, value) {
  if (!field && value) return value
  return field
}

async function handleOcr(type) {
  const fileId = certIds.value[0]
  if (!fileId) { ElMessage.warning('请先上传获奖证书再点击识别'); return }
  ocrLoading.value = true
  try {
    const res = await recognizeOcrById(fileId, type)
    if (res.code === 200 && res.data && res.data.success) {
      const d = res.data
      if (type === 'competition') {
        form.competitionName = fillIfEmpty(form.competitionName, d.name)
        form.awardLevel = fillIfEmpty(form.awardLevel, d.awardLevel)
        form.awardGrade = fillIfEmpty(form.awardGrade, d.awardRank)
        if (!form.awardDate && d.competitionDate) form.awardDate = d.competitionDate
        form.organizer = fillIfEmpty(form.organizer, d.hostUnit)
        form.advisor = fillIfEmpty(form.advisor, d.advisor)
        form.participants = fillIfEmpty(form.participants, d.submitter)
      }
      ElMessage.success('识别完成，已自动填写表单，请核对修改')
    } else {
      ElMessage.warning('识别失败：' + (res.data?.errorMessage || '请手动填写'))
    }
  } catch {
    ElMessage.error('识别服务异常，请手动填写')
  } finally {
    ocrLoading.value = false
  }
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await getCompetitionDetail(route.params.id)
      if (res.code === 200) {
        Object.assign(form, res.data)
        if (res.data.fileUrlList) {
          const byMaterial = groupByMaterial(res.data.fileUrlList)
          certFiles.value = byMaterial['获奖证书'] || []
          photoFiles.value = byMaterial['现场合影'] || []
          otherFiles.value = byMaterial['其他材料'] || []
          certIds.value = certFiles.value.map(f => f.id)
          photoIds.value = photoFiles.value.map(f => f.id)
          otherIds.value = otherFiles.value.map(f => f.id)
        }
      }
    } catch {
      ElMessage.error('加载数据失败')
      router.push('/competition/list')
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
  if (certIds.value.length === 0) { ElMessage.error('请至少上传一张获奖证书'); return }

  loading.value = true
  try {
    const fileIds = [...certIds.value, ...photoIds.value, ...otherIds.value]
    const data = { ...form, fileIds }
    const res = isEdit.value
      ? await updateCompetition(route.params.id, data)
      : await submitCompetition(data)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '提交成功')
      router.push('/competition/list')
    } else {
      ElMessage.error(res.message || '操作失败')
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
