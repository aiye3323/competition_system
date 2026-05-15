<template>
  <div>
    <div class="page-header">
      <h2>审核管理</h2>
    </div>

    <!-- Type Tabs -->
    <el-card class="card-wrapper" shadow="never">
      <el-tabs v-model="typeFilter" @tab-change="fetchData">
        <el-tab-pane label="全部" name="ALL" />
        <el-tab-pane label="学科竞赛" name="COMPETITION" />
        <el-tab-pane label="创新项目" name="PROJECT" />
        <el-tab-pane label="学术论文" name="PAPER" />
        <el-tab-pane label="软件著作" name="SOFTWARE" />
      </el-tabs>

      <!-- Status & search -->
      <el-form :inline="true" style="margin-top:12px;">
        <el-form-item label="审核状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width:140px;" @change="fetchData">
            <el-option label="待审核" value="PENDING" />
            <el-option label="待领导审核" value="PENDING_LEADER" />
            <el-option label="已归档" value="ARCHIVED" />
            <el-option label="已退回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="keyword" placeholder="搜索名称/申报人" clearable style="width:200px;" @keyup.enter="fetchData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column label="成果类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="名称/标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="applicantName" label="申报人" width="100" />
      <el-table-column prop="applyTime" label="申报时间" width="160">
        <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openDetail(row)">审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchData"
      />
    </div>

    <!-- Audit Detail Dialog -->
    <el-dialog v-model="detailVisible" title="审核详情" width="750px" top="3vh" class="audit-dialog">
      <template v-if="detail">
        <!-- Achievement Info -->
        <el-card shadow="never" class="detail-section">
          <template #header><span style="font-weight:bold;">成果信息</span></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="类型" :span="2">
              <el-tag :type="typeTag(detail.type)" size="small">{{ typeLabel(detail.type) }}</el-tag>
            </el-descriptions-item>
            <template v-if="detail.type === 'COMPETITION'">
              <el-descriptions-item label="竞赛名称" :span="2">{{ detail.fields?.competitionName }}</el-descriptions-item>
              <el-descriptions-item label="竞赛类别">{{ detail.fields?.category }}</el-descriptions-item>
              <el-descriptions-item label="获奖级别">{{ detail.fields?.awardLevel }}</el-descriptions-item>
              <el-descriptions-item label="获奖等级">{{ detail.fields?.awardGrade }}</el-descriptions-item>
              <el-descriptions-item label="获奖单位" :span="2">{{ detail.fields?.awardUnit }}</el-descriptions-item>
              <el-descriptions-item label="指导教师" :span="2">{{ detail.fields?.advisor }}</el-descriptions-item>
              <el-descriptions-item label="参赛选手" :span="2">{{ detail.fields?.participants }}</el-descriptions-item>
            </template>
            <template v-if="detail.type === 'PROJECT'">
              <el-descriptions-item label="项目名称" :span="2">{{ detail.fields?.projectName }}</el-descriptions-item>
              <el-descriptions-item label="项目级别">{{ detail.fields?.projectLevel }}</el-descriptions-item>
              <el-descriptions-item label="项目类型">{{ detail.fields?.projectType }}</el-descriptions-item>
              <el-descriptions-item label="指导教师" :span="2">{{ detail.fields?.advisor }}</el-descriptions-item>
              <el-descriptions-item label="团队成员" :span="2">{{ detail.fields?.members }}</el-descriptions-item>
            </template>
            <template v-if="detail.type === 'PAPER'">
              <el-descriptions-item label="论文标题" :span="2">{{ detail.fields?.title }}</el-descriptions-item>
              <el-descriptions-item label="期刊名称" :span="2">{{ detail.fields?.journalName }}</el-descriptions-item>
              <el-descriptions-item label="期刊级别">{{ detail.fields?.journalLevel }}</el-descriptions-item>
              <el-descriptions-item label="作者">{{ detail.fields?.authors }}</el-descriptions-item>
            </template>
            <template v-if="detail.type === 'SOFTWARE'">
              <el-descriptions-item label="软件名称" :span="2">{{ detail.fields?.softwareName }}</el-descriptions-item>
              <el-descriptions-item label="著作权人" :span="2">{{ detail.fields?.copyrightOwner }}</el-descriptions-item>
              <el-descriptions-item label="登记号">{{ detail.fields?.registrationNumber }}</el-descriptions-item>
              <el-descriptions-item label="所属单位">{{ detail.fields?.affiliation }}</el-descriptions-item>
            </template>
            <el-descriptions-item label="申报人">{{ detail.applicantName }}</el-descriptions-item>
            <el-descriptions-item label="申报时间">{{ formatTime(detail.applyTime) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- Attachments -->
        <el-card shadow="never" class="detail-section">
          <template #header><span style="font-weight:bold;">附件材料</span></template>
          <AttachmentDisplay
            :files="detail.files"
            :context-label="attachmentContextLabel"
            :related-type="detail.type"
            :related-id="detail.id"
            :achievement-name="auditAchievementName"
            :applicant-name="detail.applicantName"
          />
        </el-card>

        <!-- Audit Timeline -->
        <el-card v-if="detail.auditLogs && detail.auditLogs.length > 0" shadow="never" class="detail-section">
          <template #header><span style="font-weight:bold;">审核记录</span></template>
          <el-timeline>
            <el-timeline-item
              v-for="log in detail.auditLogs"
              :key="log.id"
              :timestamp="log.auditTime"
              :type="log.result === 'APPROVED' ? 'success' : 'danger'"
              placement="top"
            >
              <div>
                <el-tag :type="log.result === 'APPROVED' ? 'success' : 'danger'" size="small" effect="plain">
                  {{ log.result === 'APPROVED' ? '通过' : '驳回' }}
                </el-tag>
                <strong style="margin-left:6px;">{{ log.auditorName }}</strong>
                <span class="text-secondary" style="margin-left:8px;">
                  ({{ log.auditorRole === 'SECRETARY' ? '科研秘书' : '学院领导' }})
                </span>
              </div>
              <p v-if="log.opinion" class="text-regular" style="margin:6px 0 0;">意见：{{ log.opinion }}</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- Audit Action -->
        <el-card shadow="never" class="detail-section">
          <template #header><span style="font-weight:bold;">审核操作</span></template>
          <el-form ref="formRef" :model="auditForm" :rules="auditRules" label-width="100px">
            <el-form-item v-if="userRole === 'LEADER' && hasSecretaryOpinion" label="秘书意见">
              <div class="text-regular" style="padding:8px 12px; background:var(--hover-bg); border-radius:8px;">
                {{ secretaryOpinion }}
              </div>
            </el-form-item>
            <el-form-item label="审核意见" prop="opinion">
              <el-input
                v-model="auditForm.opinion"
                type="textarea"
                :rows="3"
                :placeholder="auditPlaceholder"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="success" :loading="submitting" @click="handleApprove">通过</el-button>
              <el-button type="danger" :loading="submitting" @click="handleReject">退回</el-button>
              <el-button @click="detailVisible = false">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AttachmentDisplay from '@/components/AttachmentDisplay.vue'
import {
  getUnifiedPendingList, getAuditDetail,
  secretaryApproveItem, secretaryRejectItem,
  leaderApproveItem, leaderRejectItem
} from '@/api/audit'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const typeFilter = ref('ALL')
const statusFilter = ref('')
const keyword = ref('')

const detailVisible = ref(false)
const detail = ref(null)
const formRef = ref(null)
const submitting = ref(false)
const auditForm = reactive({ opinion: '' })

const userRole = computed(() => {
  try { return JSON.parse(localStorage.getItem('userInfo') || '{}').role } catch { return '' }
})

const auditRules = {
  opinion: [{
    validator: (rule, value, callback) => {
      if (submittingAction.value === 'reject' && !value) {
        callback(new Error('退回时必须填写审核意见'))
      } else { callback() }
    }, trigger: 'blur'
  }]
}

const submittingAction = ref('')

const canApprove = computed(() => {
  if (!detail.value) return false
  const role = userRole.value
  const status = detail.value.status
  if (role === 'SECRETARY' && status === 'PENDING') return true
  if (role === 'LEADER' && status === 'PENDING_LEADER') return true
  return false
})

const hasSecretaryOpinion = computed(() => {
  if (!detail.value?.auditLogs) return false
  const secretaryLogs = detail.value.auditLogs.filter(l => l.auditorRole === 'SECRETARY' && l.opinion)
  return secretaryLogs.length > 0
})

const secretaryOpinion = computed(() => {
  if (!detail.value?.auditLogs) return ''
  const logs = detail.value.auditLogs.filter(l => l.auditorRole === 'SECRETARY' && l.opinion)
  return logs.length > 0 ? logs[logs.length - 1].opinion : ''
})

const auditPlaceholder = computed(() => {
  if (submittingAction.value === 'reject') return '请输入审核意见（退回时必填）'
  return '选填'
})

const attachmentContextLabel = computed(() => {
  if (!detail.value) return ''
  const d = detail.value
  const prefix = { COMPETITION: '竞赛', PROJECT: '项目', PAPER: '论文', SOFTWARE: '软著' }[d.type] || d.type
  let name = ''
  if (d.type === 'COMPETITION') name = d.fields?.competitionName
  else if (d.type === 'PROJECT') name = d.fields?.projectName
  else if (d.type === 'PAPER') name = d.fields?.title
  else if (d.type === 'SOFTWARE') name = d.fields?.softwareName
  return [prefix, name, d.applicantName].filter(Boolean).join('_')
})

const auditAchievementName = computed(() => {
  const d = detail.value
  if (!d) return ''
  if (d.type === 'COMPETITION') return d.fields?.competitionName || ''
  if (d.type === 'PROJECT') return d.fields?.projectName || ''
  if (d.type === 'PAPER') return d.fields?.title || ''
  if (d.type === 'SOFTWARE') return d.fields?.softwareName || ''
  return ''
})

onMounted(() => {
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const params = {
      role: userRole.value || 'SECRETARY',
      type: typeFilter.value,
      page: page.value,
      size: size.value
    }
    if (statusFilter.value) params.status = statusFilter.value
    if (keyword.value) params.keyword = keyword.value
    const res = await getUnifiedPendingList(params)
    if (res.code === 200) {
      list.value = res.data.content
      total.value = res.data.totalElements
    }
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  auditForm.opinion = ''
  detail.value = null
  detailVisible.value = true
  try {
    const res = await getAuditDetail(row.type, row.id)
    if (res.code === 200) {
      detail.value = res.data
    }
  } catch {
    ElMessage.error('获取详情失败')
    detailVisible.value = false
  }
}

async function handleApprove() {
  submittingAction.value = 'approve'
  if (!await validateForm()) return
  await doAudit('approve')
}

async function handleReject() {
  submittingAction.value = 'reject'
  if (!await validateForm()) return
  await doAudit('reject')
}

async function validateForm() {
  try {
    await formRef.value.validate()
    return true
  } catch {
    return false
  }
}

async function doAudit(action) {
  if (!detail.value) return
  submitting.value = true
  try {
    const role = userRole.value
    const data = { opinion: auditForm.opinion }
    let res
    if (role === 'SECRETARY') {
      res = action === 'approve'
        ? await secretaryApproveItem(detail.value.type, detail.value.id, data)
        : await secretaryRejectItem(detail.value.type, detail.value.id, data)
    } else {
      res = action === 'approve'
        ? await leaderApproveItem(detail.value.type, detail.value.id, data)
        : await leaderRejectItem(detail.value.type, detail.value.id, data)
    }
    if (res.code === 200) {
      ElMessage.success(action === 'approve' ? '审核通过' : '已退回')
      detailVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

function typeTag(type) {
  return { COMPETITION: '', PROJECT: 'success', PAPER: 'warning', SOFTWARE: 'info' }[type] || ''
}

function typeLabel(type) {
  return { COMPETITION: '竞赛', PROJECT: '项目', PAPER: '论文', SOFTWARE: '软著' }[type] || type
}

function statusTag(status) {
  return { PENDING: 'warning', PENDING_LEADER: 'primary', ARCHIVED: 'success', REJECTED: 'danger' }[status] || 'info'
}

function statusLabel(status) {
  return { PENDING: '待审核', PENDING_LEADER: '待领导审核', ARCHIVED: '已归档', REJECTED: '已退回' }[status] || status
}

function formatTime(time) {
  if (!time) return ''
  return time.substring(0, 19).replace('T', ' ')
}
</script>

<style scoped>
.detail-section {
  margin-bottom: 16px;
}
.file-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.file-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 120px;
}
.file-thumb {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid var(--border-color);
}
.file-name {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
  text-align: center;
  word-break: break-all;
  max-width: 120px;
}
</style>
