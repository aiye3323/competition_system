<template>
  <div class="submit-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑软件著作权' : '提交软件著作权' }}</h2>
      <p class="header-desc">填写软件信息并上传申报材料、证书扫描件等证明材料</p>
    </div>

    <el-card class="submit-card">
      <div class="form-section">
        <h3 class="section-title">基本信息</h3>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-form-item label="软件名称" prop="softwareName">
            <el-input v-model="form.softwareName" placeholder="请输入软件名称" />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="所属单位" prop="affiliation">
                <el-select v-model="form.affiliation" placeholder="请选择">
                  <el-option label="怀化学院" value="怀化学院" /><el-option label="其他单位" value="其他单位" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="著作权人" prop="copyrightOwner">
                <el-input v-model="form.copyrightOwner" placeholder="著作权人" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="登记日期" prop="registrationDate">
                <el-date-picker v-model="form.registrationDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="16">
              <el-form-item label="登记号" prop="registrationNumber">
                <el-input v-model="form.registrationNumber" placeholder="如 2025SR11569875" />
                <div class="field-hint">格式：4位年份 + SR + 8位数字</div>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <el-divider />

      <div class="form-section">
        <h3 class="section-title">证明材料</h3>
        <el-form label-width="100px">
          <el-form-item label="申报材料" required>
            <FileUpload :area-config="softwareAreas[0]" :existing-file-ids="materialFiles" material-type="申报材料" @update:file-ids="materialIds = $event" />
          </el-form-item>
          <el-form-item label="证书扫描件" required>
            <div style="display:flex;align-items:flex-start;gap:8px;width:100%;">
              <FileUpload :area-config="softwareAreas[1]" :existing-file-ids="certFiles" material-type="证书扫描件" @update:file-ids="certIds = $event" style="flex:1;" />
              <el-button size="small" :loading="ocrLoading" @click="handleOcr('software')" :disabled="certIds.length === 0">识别填充</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <div class="form-actions">
        <el-button type="primary" :loading="loading" @click="handleSubmit">提交</el-button>
        <el-button @click="$router.push('/software/list')">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitSoftware, getSoftwareDetail, updateSoftware } from '@/api/software'
import FileUpload from '@/components/FileUpload.vue'
import { softwareAreas } from '@/utils/uploadConfig'
import { recognizeOcrById } from '@/api/ocr'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const ocrLoading = ref(false)
const materialIds = ref([])
const certIds = ref([])
const materialFiles = ref([])
const certFiles = ref([])
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  softwareName: '', affiliation: '', copyrightOwner: '',
  registrationNumber: '', registrationDate: ''
})

const rules = {
  softwareName: [{ required: true, message: '请输入软件名称', trigger: 'blur' }],
  affiliation: [{ required: true, message: '请选择所属单位', trigger: 'change' }],
  copyrightOwner: [{ required: true, message: '请输入著作权人', trigger: 'blur' }],
  registrationNumber: [{ required: true, message: '请输入登记号', trigger: 'blur' }],
  registrationDate: [{ required: true, message: '请选择登记日期', trigger: 'change' }]
}

function fillIfEmpty(f, v) { return (!f && v) ? v : f }

async function handleOcr(type) {
  const fileId = certIds.value[0]
  if (!fileId) { ElMessage.warning('请先上传证书扫描件再点击识别'); return }
  ocrLoading.value = true
  try {
    const res = await recognizeOcrById(fileId, type)
    if (res.code === 200 && res.data?.success) {
      const d = res.data
      form.softwareName = fillIfEmpty(form.softwareName, d.softwareName || d.name)
      form.copyrightOwner = fillIfEmpty(form.copyrightOwner, d.copyrightHolder)
      form.registrationNumber = fillIfEmpty(form.registrationNumber, d.registrationNo)
      if (!form.registrationDate && d.registrationDate) form.registrationDate = d.registrationDate
      ElMessage.success('识别完成，已自动填写表单，请核对修改')
    } else { ElMessage.warning('识别失败：' + (res.data?.errorMessage || '请手动填写')) }
  } catch { ElMessage.error('识别服务异常，请手动填写') }
  finally { ocrLoading.value = false }
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await getSoftwareDetail(route.params.id)
      if (res.code === 200) {
        Object.assign(form, res.data)
        if (res.data.fileUrlList) {
          const byMaterial = groupByMaterial(res.data.fileUrlList)
          materialFiles.value = byMaterial['申报材料'] || []
          certFiles.value = byMaterial['证书扫描件'] || []
          materialIds.value = materialFiles.value.map(f => f.id)
          certIds.value = certFiles.value.map(f => f.id)
        }
      }
    } catch {
      ElMessage.error('加载数据失败')
      router.push('/software/list')
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
  if (materialIds.value.length === 0) { ElMessage.error('请上传申报材料'); return }
  if (certIds.value.length === 0) { ElMessage.error('请上传证书扫描件'); return }

  loading.value = true
  try {
    const fileIds = [...materialIds.value, ...certIds.value]
    const data = { ...form, fileIds }
    const res = isEdit.value
      ? await updateSoftware(route.params.id, data)
      : await submitSoftware(data)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '提交成功')
      router.push('/software/list')
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

.submit-card {
  border-radius: 12px;
}

:deep(.el-card__body) {
  padding: 32px 40px;
}

.form-section { margin-bottom: var(--space-lg); }
.section-title { font-size: var(--text-base); font-weight: 600; color: var(--text-primary); margin: 0 0 var(--space-lg); }

:deep(.el-form-item) {
  margin-bottom: 22px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.el-input__wrapper) {
  border-radius: 6px;
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
  border-radius: 8px;
}

.field-hint { font-size: var(--text-xs); color: var(--text-secondary); margin-top: 4px; }

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
