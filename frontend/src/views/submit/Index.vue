<template>
  <div style="max-width:850px; margin:0 auto;">
    <h2>提交成果</h2>

    <!-- 步骤引导 -->
    <div class="submit-steps">
      <div class="step active"><span class="step-num">1</span> 选择类型</div>
      <div class="step-arrow">→</div>
      <div class="step"><span class="step-num">2</span> 填写信息</div>
      <div class="step-arrow">→</div>
      <div class="step"><span class="step-num">3</span> 上传材料</div>
      <div class="step-arrow">→</div>
      <div class="step"><span class="step-num">4</span> 提交审核</div>
    </div>

    <!-- 类型选择 -->
    <el-card style="margin:16px 0;">
      <template #header><span style="font-weight:600">第1步：选择成果类型</span></template>
      <el-radio-group v-model="currentType" size="large" @change="onTypeChange">
        <el-radio-button value="competition">学科竞赛</el-radio-button>
        <el-radio-button value="project">创新项目</el-radio-button>
        <el-radio-button value="paper">学术论文</el-radio-button>
        <el-radio-button value="software">软件著作</el-radio-button>
      </el-radio-group>
    </el-card>

    <!-- ===================== 学科竞赛表单 ===================== -->
    <el-card v-if="currentType === 'competition'">
      <template #header><span style="font-weight:bold;">学科竞赛成果</span></template>
      <el-form ref="compFormRef" :model="compForm" :rules="compRules" label-width="100px">
        <el-form-item label="竞赛类别" prop="category">
          <el-select v-model="compForm.category" placeholder="请选择" style="width:100%">
            <el-option label="A类" value="A类" /><el-option label="B类" value="B类" /><el-option label="C类" value="C类" />
          </el-select>
        </el-form-item>
        <el-form-item label="竞赛名称" prop="competitionName">
          <el-input v-model="compForm.competitionName" placeholder="请输入竞赛名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="获奖级别" prop="awardLevel">
              <el-select v-model="compForm.awardLevel" placeholder="请选择" style="width:100%">
                <el-option label="国家级" value="国家级" /><el-option label="省级" value="省级" />
                <el-option label="市级" value="市级" /><el-option label="校级" value="校级" /><el-option label="院级" value="院级" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="获奖等级" prop="awardGrade">
              <el-select v-model="compForm.awardGrade" placeholder="请选择" style="width:100%">
                <el-option label="一等奖" value="一等奖" /><el-option label="二等奖" value="二等奖" />
                <el-option label="三等奖" value="三等奖" /><el-option label="优秀奖" value="优秀奖" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="获奖单位" prop="awardUnit">
          <el-input v-model="compForm.awardUnit" placeholder="请输入颁奖单位" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="主办单位"><el-input v-model="compForm.organizer" placeholder="主办单位" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="承办单位"><el-input v-model="compForm.coOrganizer" placeholder="承办单位" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="获奖时间"><el-date-picker v-model="compForm.awardDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="指导教师"><el-input v-model="compForm.advisor" placeholder="多位教师用逗号分隔" /></el-form-item>
        <el-form-item label="参赛选手"><el-input v-model="compForm.participants" type="textarea" :rows="2" placeholder="多人用逗号分隔" /></el-form-item>
        <el-divider>证明材料</el-divider>
        <el-form-item label="获奖证书">
          <FileUpload material-type="获奖证书" :area-config="competitionAreas[0]" @update:file-ids="compCertIds = $event" />
        </el-form-item>
        <el-form-item label="现场合影">
          <FileUpload material-type="现场合影" :area-config="competitionAreas[1]" @update:file-ids="compPhotoIds = $event" />
        </el-form-item>
        <el-form-item label="其他材料">
          <FileUpload material-type="其他材料" :area-config="competitionAreas[2]" @update:file-ids="compOtherIds = $event" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmitCompetition">提交竞赛成果</el-button>
          <el-button @click="resetCompForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ===================== 创新项目表单 ===================== -->
    <el-card v-if="currentType === 'project'">
      <template #header><span style="font-weight:bold;">大学生创新训练计划项目</span></template>
      <el-form ref="projFormRef" :model="projForm" :rules="projRules" label-width="100px">
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="projForm.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="立项级别" prop="projectLevel">
              <el-select v-model="projForm.projectLevel" placeholder="请选择" style="width:100%">
                <el-option label="国家级" value="国家级" /><el-option label="省级" value="省级" />
                <el-option label="校级" value="校级" /><el-option label="院级" value="院级" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="立项类型" prop="projectType">
              <el-select v-model="projForm.projectType" placeholder="请选择" style="width:100%">
                <el-option label="创新训练" value="创新训练" /><el-option label="创业训练" value="创业训练" /><el-option label="创业实践" value="创业实践" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="指导教师"><el-input v-model="projForm.advisor" placeholder="多位教师用逗号分隔" /></el-form-item>
        <el-form-item label="立项人员"><el-input v-model="projForm.members" type="textarea" :rows="2" placeholder="多人用逗号分隔" /></el-form-item>
        <el-form-item label="立项时间"><el-date-picker v-model="projForm.establishTime" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item>
        <el-divider>证明材料</el-divider>
        <el-form-item label="立项申报书">
          <FileUpload material-type="立项申报书" :area-config="projectAreas[0]" @update:file-ids="projProposalIds = $event" />
        </el-form-item>
        <el-form-item label="结题材料">
          <FileUpload material-type="结题材料" :area-config="projectAreas[1]" @update:file-ids="projConclusionIds = $event" />
        </el-form-item>
        <el-form-item label="结题证书">
          <FileUpload material-type="结题证书" :area-config="projectAreas[2]" @update:file-ids="projCertIds = $event" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmitProject">提交创新项目</el-button>
          <el-button @click="resetProjForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ===================== 学术论文表单 ===================== -->
    <el-card v-if="currentType === 'paper'">
      <template #header><span style="font-weight:bold;">学术论文成果</span></template>
      <el-form ref="paperFormRef" :model="paperForm" :rules="paperRules" label-width="100px">
        <el-form-item label="论文标题" prop="title">
          <el-input v-model="paperForm.title" placeholder="请输入论文标题" />
        </el-form-item>
        <el-form-item label="期刊名称"><el-input v-model="paperForm.journalName" placeholder="请输入期刊或会议名称" /></el-form-item>
        <el-form-item label="期刊级别" prop="journalLevel">
          <el-select v-model="paperForm.journalLevel" placeholder="请选择" style="width:100%">
            <el-option label="SCI一区" value="SCI一区" /><el-option label="SCI二区" value="SCI二区" />
            <el-option label="SCI三区" value="SCI三区" /><el-option label="SCI四区" value="SCI四区" />
            <el-option label="EI期刊" value="EI期刊" /><el-option label="CCF A类会议" value="CCF A类会议" />
            <el-option label="CCF B类会议" value="CCF B类会议" /><el-option label="CCF C类会议" value="CCF C类会议" />
            <el-option label="EI会议" value="EI会议" />
            <el-option label="北大核心期刊" value="北大核心期刊" /><el-option label="省级期刊" value="省级期刊" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词"><el-input v-model="paperForm.keywords" placeholder="多个关键词用逗号分隔" /></el-form-item>
        <el-form-item label="作者列表"><el-input v-model="paperForm.authors" type="textarea" :rows="2" placeholder="多人用逗号分隔" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="投稿日期"><el-date-picker v-model="paperForm.submissionDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="录用日期"><el-date-picker v-model="paperForm.acceptanceDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item>
          </el-col>
        </el-row>
        <el-divider>证明材料</el-divider>
        <el-form-item label="投稿初稿">
          <FileUpload material-type="投稿初稿" :area-config="paperAreas[0]" @update:file-ids="paperDraftIds = $event" />
        </el-form-item>
        <el-form-item label="录用终稿">
          <FileUpload material-type="录用终稿" :area-config="paperAreas[1]" @update:file-ids="paperFinalIds = $event" />
        </el-form-item>
        <el-form-item label="审稿意见">
          <FileUpload material-type="审稿意见" :area-config="paperAreas[2]" @update:file-ids="paperReviewIds = $event" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmitPaper">提交论文成果</el-button>
          <el-button @click="resetPaperForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ===================== 软件著作表单 ===================== -->
    <el-card v-if="currentType === 'software'">
      <template #header><span style="font-weight:bold;">软件著作权</span></template>
      <el-form ref="swFormRef" :model="swForm" :rules="swRules" label-width="100px">
        <el-form-item label="软件名称" prop="softwareName">
          <el-input v-model="swForm.softwareName" placeholder="请输入软件名称" />
        </el-form-item>
        <el-form-item label="所属单位" prop="affiliation">
          <el-select v-model="swForm.affiliation" placeholder="请选择" style="width:100%">
            <el-option label="怀化学院" value="怀化学院" /><el-option label="其他单位" value="其他单位" />
          </el-select>
        </el-form-item>
        <el-form-item label="著作权人" prop="copyrightOwner">
          <el-input v-model="swForm.copyrightOwner" placeholder="请输入著作权人" />
        </el-form-item>
        <el-form-item label="登记号" prop="registrationNumber">
          <el-input v-model="swForm.registrationNumber" placeholder="如 2025SR11569875" />
          <div style="font-size:12px; color:#909399; margin-top:4px;">格式：4位年份 + SR + 8位数字，如 2025SR11569875</div>
        </el-form-item>
        <el-form-item label="登记日期" prop="registrationDate">
          <el-date-picker v-model="swForm.registrationDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-divider>证明材料</el-divider>
        <el-form-item label="申报材料">
          <FileUpload material-type="申报材料" :area-config="softwareAreas[0]" @update:file-ids="swMaterialIds = $event" />
        </el-form-item>
        <el-form-item label="证书扫描件">
          <FileUpload material-type="证书扫描件" :area-config="softwareAreas[1]" @update:file-ids="swCertIds = $event" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmitSoftware">提交软件著作</el-button>
          <el-button @click="resetSwForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { submitCompetition } from '@/api/competition'
import { submitProject } from '@/api/project'
import { submitPaper } from '@/api/paper'
import { submitSoftware } from '@/api/software'
import FileUpload from '@/components/FileUpload.vue'
import { competitionAreas, projectAreas, paperAreas, softwareAreas } from '@/utils/uploadConfig'

const currentType = ref('competition')
const submitting = ref(false)

// ========== 竞赛表单 ==========
const compFormRef = ref(null)
const compCertIds = ref([]), compPhotoIds = ref([]), compOtherIds = ref([])
const compForm = reactive({ category: '', competitionName: '', awardLevel: '', awardGrade: '', awardUnit: '', organizer: '', coOrganizer: '', awardDate: '', advisor: '', participants: '' })
const compRules = {
  category: [{ required: true, message: '请选择竞赛类别', trigger: 'change' }],
  competitionName: [{ required: true, message: '请输入竞赛名称', trigger: 'blur' }],
  awardLevel: [{ required: true, message: '请选择获奖级别', trigger: 'change' }],
  awardGrade: [{ required: true, message: '请选择获奖等级', trigger: 'change' }]
}
async function handleSubmitCompetition() {
  const valid = await compFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await submitCompetition({ ...compForm, fileIds: [...compCertIds.value, ...compPhotoIds.value, ...compOtherIds.value] })
    if (res.code === 200) { ElMessage.success('竞赛成果提交成功'); resetCompForm() }
    else ElMessage.error(res.message || '提交失败')
  } finally { submitting.value = false }
}
function resetCompForm() { compFormRef.value?.resetFields(); compCertIds.value = []; compPhotoIds.value = []; compOtherIds.value = [] }

// ========== 项目表单 ==========
const projFormRef = ref(null)
const projProposalIds = ref([]), projConclusionIds = ref([]), projCertIds = ref([])
const projForm = reactive({ projectName: '', projectLevel: '', projectType: '', advisor: '', members: '', establishTime: '' })
const projRules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectLevel: [{ required: true, message: '请选择立项级别', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择立项类型', trigger: 'change' }]
}
async function handleSubmitProject() {
  const valid = await projFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await submitProject({ ...projForm, fileIds: [...projProposalIds.value, ...projConclusionIds.value, ...projCertIds.value] })
    if (res.code === 200) { ElMessage.success('创新项目提交成功'); resetProjForm() }
    else ElMessage.error(res.message || '提交失败')
  } finally { submitting.value = false }
}
function resetProjForm() { projFormRef.value?.resetFields(); projProposalIds.value = []; projConclusionIds.value = []; projCertIds.value = [] }

// ========== 论文表单 ==========
const paperFormRef = ref(null)
const paperDraftIds = ref([]), paperFinalIds = ref([]), paperReviewIds = ref([])
const paperForm = reactive({ title: '', journalName: '', journalLevel: '', keywords: '', authors: '', submissionDate: '', acceptanceDate: '' })
const paperRules = {
  title: [{ required: true, message: '请输入论文标题', trigger: 'blur' }],
  journalLevel: [{ required: true, message: '请选择期刊级别', trigger: 'change' }]
}
async function handleSubmitPaper() {
  const valid = await paperFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await submitPaper({ ...paperForm, fileIds: [...paperDraftIds.value, ...paperFinalIds.value, ...paperReviewIds.value] })
    if (res.code === 200) { ElMessage.success('论文成果提交成功'); resetPaperForm() }
    else ElMessage.error(res.message || '提交失败')
  } finally { submitting.value = false }
}
function resetPaperForm() { paperFormRef.value?.resetFields(); paperDraftIds.value = []; paperFinalIds.value = []; paperReviewIds.value = [] }

// ========== 软著表单 ==========
const swFormRef = ref(null)
const swMaterialIds = ref([]), swCertIds = ref([])
const swForm = reactive({ softwareName: '', affiliation: '', copyrightOwner: '', registrationNumber: '', registrationDate: '' })
const swRules = {
  softwareName: [{ required: true, message: '请输入软件名称', trigger: 'blur' }],
  affiliation: [{ required: true, message: '请选择所属单位', trigger: 'change' }],
  copyrightOwner: [{ required: true, message: '请输入著作权人', trigger: 'blur' }],
  registrationNumber: [
    { required: true, message: '请输入登记号', trigger: 'blur' },
    { pattern: /^\d{4}SR\d{8}$/, message: '登记号格式：4位年份+SR+8位数字，如 2025SR11569875', trigger: 'blur' }
  ]
}
async function handleSubmitSoftware() {
  const valid = await swFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await submitSoftware({ ...swForm, fileIds: [...swMaterialIds.value, ...swCertIds.value] })
    if (res.code === 200) { ElMessage.success('软件著作提交成功'); resetSwForm() }
    else ElMessage.error(res.message || '提交失败')
  } finally { submitting.value = false }
}
function resetSwForm() { swFormRef.value?.resetFields(); swMaterialIds.value = []; swCertIds.value = [] }

function onTypeChange() {
  // 切换类型时不做额外处理
}
</script>

<style scoped>
.submit-steps {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--card-radius);
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.step {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);
}

.step.active {
  color: var(--primary-color);
  font-weight: 600;
}

.step-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--border-light);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.step.active .step-num {
  background: var(--primary-color);
  color: #fff;
}

.step-arrow {
  color: var(--text-placeholder);
  font-size: 14px;
}
</style>
