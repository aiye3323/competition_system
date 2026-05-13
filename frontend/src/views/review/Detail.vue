<template>
  <div class="page-container">
    <el-button text @click="$router.push('/review/list')">&lt; 返回审核列表</el-button>
    <h2>审核详情</h2>

    <el-card v-if="detail" class="card-wrapper">
      <template #header><span>竞赛信息</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="竞赛名称" :span="2">{{ detail.competitionName }}</el-descriptions-item>
        <el-descriptions-item label="竞赛类别">{{ detail.category }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="statusType(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="获奖级别">{{ detail.awardLevel }}</el-descriptions-item>
        <el-descriptions-item label="获奖等级">{{ detail.awardGrade }}</el-descriptions-item>
        <el-descriptions-item label="获奖单位" :span="2">{{ detail.awardUnit }}</el-descriptions-item>
        <el-descriptions-item label="申报人">{{ detail.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="申报时间">{{ detail.applyTime }}</el-descriptions-item>
        <el-descriptions-item label="指导教师" :span="2">{{ detail.advisor }}</el-descriptions-item>
        <el-descriptions-item label="参赛选手" :span="2">{{ detail.participants }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="auditLogs.length > 0" class="card-wrapper">
      <template #header><span>审核记录</span></template>
      <el-timeline>
        <el-timeline-item
          v-for="log in auditLogs"
          :key="log.id"
          :timestamp="log.auditTime"
          :type="log.result === 'APPROVED' ? 'success' : 'danger'"
          placement="top"
        >
          <div>
            <el-tag :type="log.result === 'APPROVED' ? 'success' : 'danger'" size="small" effect="plain">
              {{ log.result === 'APPROVED' ? '通过' : '驳回' }}
            </el-tag>
            <strong>{{ log.auditorName }}</strong>
            <span class="text-secondary" style="margin-left:8px;">
              ({{ log.auditorRole === 'SECRETARY' ? '科研秘书' : '学院领导' }})
            </span>
          </div>
          <p v-if="log.opinion" style="margin:8px 0 0;" class="text-secondary">意见：{{ log.opinion }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-card v-if="canReview" class="card-wrapper">
      <template #header><span>审核操作</span></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="审核结果" prop="result">
          <el-radio-group v-model="form.result">
            <el-radio value="APPROVED">通过</el-radio>
            <el-radio value="REJECTED">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" prop="opinion">
          <el-input
            v-model="form.opinion"
            type="textarea"
            :rows="4"
            placeholder="请输入审核意见（驳回时必填）"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交审核</el-button>
          <el-button @click="$router.push('/review/list')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCompetitionDetail } from '@/api/competition'
import { secretaryReview, leaderReview, getAuditLogs } from '@/api/review'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const detail = ref(null)
const auditLogs = ref([])
const formRef = ref(null)
const submitting = ref(false)

const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

const canReview = computed(() => {
  if (!detail.value) return false
  const role = userInfo.value.role
  const status = detail.value.status
  if (role === 'SECRETARY' && status === 'PENDING') return true
  if (role === 'LEADER' && status === 'FIRST_REVIEW') return true
  return false
})

const form = ref({
  result: 'APPROVED',
  opinion: ''
})

// 驳回时 opinion 必填
const rules = {
  result: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  opinion: [
    {
      validator: (rule, value, callback) => {
        if (form.value.result === 'REJECTED' && !value) {
          callback(new Error('驳回时请填写审核意见'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 切换结果时重新校验意见
watch(() => form.value.result, () => {
  if (formRef.value) formRef.value.validateField('opinion')
})

onMounted(async () => {
  const res = await getCompetitionDetail(id)
  if (res.code === 200) {
    detail.value = res.data
  }
  const logsRes = await getAuditLogs(id)
  if (logsRes.code === 200) {
    auditLogs.value = logsRes.data
  }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const role = userInfo.value.role
    const data = { result: form.value.result, opinion: form.value.opinion }
    let res
    if (role === 'SECRETARY') {
      res = await secretaryReview(id, data)
    } else {
      res = await leaderReview(id, data)
    }
    if (res.code === 200) {
      ElMessage.success('审核完成')
      router.push('/review/list')
    } else {
      ElMessage.error(res.message || '审核失败')
    }
  } finally {
    submitting.value = false
  }
}

function statusType(status) {
  const map = { DRAFT: 'info', PENDING: 'warning', FIRST_REVIEW: '', FINAL_REVIEW: 'primary', APPROVED: 'success', REJECTED: 'danger', ARCHIVED: '' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { DRAFT: '草稿', PENDING: '待审核', FIRST_REVIEW: '一审中', FINAL_REVIEW: '终审中', APPROVED: '已通过', REJECTED: '已驳回', ARCHIVED: '已归档' }
  return map[status] || status
}
</script>
