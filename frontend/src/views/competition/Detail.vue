<template>
  <div class="page-container">
    <el-button text @click="$router.push('/competition/list')">&lt; 返回列表</el-button>
    <h2>竞赛详情</h2>

    <el-card v-if="detail" class="card-wrapper">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="竞赛名称" :span="2">{{ detail.competitionName }}</el-descriptions-item>
        <el-descriptions-item label="竞赛类别">{{ detail.category }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="statusType(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="获奖级别">{{ detail.awardLevel }}</el-descriptions-item>
        <el-descriptions-item label="获奖等级">{{ detail.awardGrade }}</el-descriptions-item>
        <el-descriptions-item label="获奖单位" :span="2">{{ detail.awardUnit }}</el-descriptions-item>
        <el-descriptions-item label="主办单位" :span="2">{{ detail.organizer }}</el-descriptions-item>
        <el-descriptions-item label="承办单位" :span="2">{{ detail.coOrganizer }}</el-descriptions-item>
        <el-descriptions-item label="获奖时间">{{ detail.awardDate }}</el-descriptions-item>
        <el-descriptions-item label="申报人">{{ detail.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="指导教师" :span="2">{{ detail.advisor }}</el-descriptions-item>
        <el-descriptions-item label="参赛选手" :span="2">{{ detail.participants }}</el-descriptions-item>
        <el-descriptions-item label="申报时间">{{ detail.applyTime }}</el-descriptions-item>
        <el-descriptions-item label="归档时间">{{ detail.archiveTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top:24px;">
        <h3>证明材料</h3>
        <AttachmentDisplay
          :file-urls="detail.fileUrlList"
          :context-label="contextLabel"
          related-type="COMPETITION"
          :related-id="detail?.id"
          :achievement-name="detail?.competitionName"
          :applicant-name="detail?.applicantName"
        />
      </div>
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

    <div v-if="detail && detail.status === 'REJECTED'" style="margin-top:20px;">
      <el-button type="primary" @click="$router.push(`/competition/edit/${id}`)">修改后重新提交</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCompetitionDetail } from '@/api/competition'
import { getAuditLogs } from '@/api/review'
import AttachmentDisplay from '@/components/AttachmentDisplay.vue'

const route = useRoute()
const id = route.params.id
const detail = ref(null)
const auditLogs = ref([])

const contextLabel = computed(() => {
  if (!detail.value) return ''
  return ['竞赛', detail.value.applicantName, detail.value.competitionName].filter(Boolean).join('_')
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

function statusType(status) {
  const map = { DRAFT: 'info', PENDING: 'warning', PENDING_LEADER: "warning", FIRST_REVIEW: "", FINAL_REVIEW: 'primary', APPROVED: 'success', REJECTED: 'danger', ARCHIVED: '' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { DRAFT: '草稿', PENDING: '待审核', PENDING_LEADER: "领导审核中", FIRST_REVIEW: "一审中", FINAL_REVIEW: '终审中', APPROVED: '已通过', REJECTED: '已驳回', ARCHIVED: '已归档' }
  return map[status] || status
}
</script>
