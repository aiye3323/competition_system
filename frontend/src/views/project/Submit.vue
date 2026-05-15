<template>
  <div class="submit-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑创新项目' : '提交创新项目' }}</h2>
      <p class="header-desc">填写项目信息并上传申报书、结题材料等证明材料</p>
    </div>

    <el-card class="submit-card">
      <div class="form-section">
        <h3 class="section-title">基本信息</h3>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-form-item label="项目名称" prop="projectName">
            <el-input v-model="form.projectName" placeholder="请输入项目名称" />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="立项级别" prop="projectLevel">
                <el-select v-model="form.projectLevel" placeholder="请选择">
                  <el-option label="国家级" value="国家级" /><el-option label="省级" value="省级" />
                  <el-option label="校级" value="校级" /><el-option label="院级" value="院级" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="立项类型" prop="projectType">
                <el-select v-model="form.projectType" placeholder="请选择">
                  <el-option label="创新训练" value="创新训练" /><el-option label="创业训练" value="创业训练" /><el-option label="创业实践" value="创业实践" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="立项时间" prop="establishTime">
                <el-date-picker v-model="form.establishTime" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="指导教师" prop="advisor">
                <el-input v-model="form.advisor" placeholder="多位用逗号分隔" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="立项人员" prop="members">
            <el-input v-model="form.members" type="textarea" :rows="2" placeholder="多人用逗号分隔" />
          </el-form-item>
        </el-form>
      </div>

      <el-divider />

      <div class="form-section">
        <h3 class="section-title">证明材料</h3>
        <el-form label-width="100px">
          <el-form-item label="立项申报书" required>
            <FileUpload :area-config="projectAreas[0]" :existing-file-ids="proposalFiles" material-type="立项申报书" @update:file-ids="proposalIds = $event" />
          </el-form-item>
          <el-form-item label="结题材料" required>
            <FileUpload :area-config="projectAreas[1]" :existing-file-ids="conclusionFiles" material-type="结题材料" @update:file-ids="conclusionIds = $event" />
          </el-form-item>
          <el-form-item label="结题证书" required>
            <div style="display:flex;align-items:flex-start;gap:8px;width:100%;">
              <FileUpload :area-config="projectAreas[2]" :existing-file-ids="certFiles" material-type="结题证书" @update:file-ids="projCertIds = $event" style="flex:1;" />
              <el-button size="small" :loading="ocrLoading" @click="handleOcr('project')" :disabled="projCertIds.length === 0">识别填充</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <div class="form-actions">
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '提交' }}
        </el-button>
        <el-button @click="$router.push('/project/list')">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitProject, getProjectDetail, updateProject } from '@/api/project'
import FileUpload from '@/components/FileUpload.vue'
import { projectAreas } from '@/utils/uploadConfig'
import { recognizeOcrById } from '@/api/ocr'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const ocrLoading = ref(false)
const proposalIds = ref([])
const conclusionIds = ref([])
const projCertIds = ref([])
const proposalFiles = ref([])
const conclusionFiles = ref([])
const certFiles = ref([])
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  projectName: '', projectLevel: '', projectType: '',
  advisor: '', members: '', establishTime: ''
})

const rules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectLevel: [{ required: true, message: '请选择立项级别', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择立项类型', trigger: 'change' }],
  advisor: [{ required: true, message: '请输入指导教师', trigger: 'blur' }],
  members: [{ required: true, message: '请输入立项人员', trigger: 'blur' }],
  establishTime: [{ required: true, message: '请选择立项时间', trigger: 'change' }]
}

function fillIfEmpty(f, v) { return (!f && v) ? v : f }

async function handleOcr(type) {
  const fileId = projCertIds.value[0]
  if (!fileId) { ElMessage.warning('请先上传结题证书再点击识别'); return }
  ocrLoading.value = true
  try {
    const res = await recognizeOcrById(fileId, type)
    if (res.code === 200 && res.data?.success) {
      const d = res.data
      form.projectName = fillIfEmpty(form.projectName, d.name)
      form.projectLevel = fillIfEmpty(form.projectLevel, d.projectLevel)
      form.projectType = fillIfEmpty(form.projectType, d.projectType)
      form.advisor = fillIfEmpty(form.advisor, d.advisor)
      form.members = fillIfEmpty(form.members, d.submitter)
      if (!form.establishTime && d.establishTime) form.establishTime = d.establishTime
      ElMessage.success('识别完成，已自动填写表单，请核对修改')
    } else { ElMessage.warning('识别失败：' + (res.data?.errorMessage || '请手动填写')) }
  } catch { ElMessage.error('识别服务异常，请手动填写') }
  finally { ocrLoading.value = false }
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await getProjectDetail(route.params.id)
      if (res.code === 200) {
        Object.assign(form, res.data)
        if (res.data.fileUrlList) {
          const byMaterial = groupByMaterial(res.data.fileUrlList)
          proposalFiles.value = byMaterial['立项申报书'] || []
          conclusionFiles.value = byMaterial['结题材料'] || []
          certFiles.value = byMaterial['结题证书'] || []
          proposalIds.value = proposalFiles.value.map(f => f.id)
          conclusionIds.value = conclusionFiles.value.map(f => f.id)
          projCertIds.value = certFiles.value.map(f => f.id)
        }
      }
    } catch {
      ElMessage.error('加载数据失败')
      router.push('/project/list')
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
  if (proposalIds.value.length === 0) { ElMessage.error('请上传立项申报书'); return }
  if (conclusionIds.value.length === 0) { ElMessage.error('请上传结题材料'); return }
  if (projCertIds.value.length === 0) { ElMessage.error('请上传结题证书'); return }

  loading.value = true
  try {
    const fileIds = [...proposalIds.value, ...conclusionIds.value, ...projCertIds.value]
    const data = { ...form, fileIds }
    const res = isEdit.value
      ? await updateProject(route.params.id, data)
      : await submitProject(data)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '提交成功')
      router.push('/project/list')
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
