<template>
  <div class="page-container">
    <el-button text @click="$router.push('/paper/list')">&lt; 返回列表</el-button>
    <h2>论文详情</h2>

    <el-card v-if="detail" class="card-wrapper">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="论文标题" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="期刊/会议名称">{{ detail.journalName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="statusType(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="期刊级别">{{ detail.journalLevel }}</el-descriptions-item>
        <el-descriptions-item label="关键词">{{ detail.keywords || '-' }}</el-descriptions-item>
        <el-descriptions-item label="作者列表" :span="2">{{ detail.authors || '-' }}</el-descriptions-item>
        <el-descriptions-item label="投稿日期">{{ detail.submissionDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="录用日期">{{ detail.acceptanceDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申报人">{{ detail.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="申报时间">{{ detail.applyTime }}</el-descriptions-item>
        <el-descriptions-item label="归档时间" :span="2">{{ detail.archiveTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top:24px;">
        <h3>证明材料</h3>
        <AttachmentDisplay
          :file-urls="detail.fileUrlList"
          :context-label="contextLabel"
          related-type="PAPER"
          :related-id="detail?.id"
        />
      </div>
    </el-card>

    <el-card v-if="auditLogs.length > 0" class="card-wrapper">
      <template #header><span>审核记录</span></template>
      <el-timeline>
        <el-timeline-item v-for="log in auditLogs" :key="log.id" :timestamp="log.auditTime"
          :type="log.result === 'APPROVED' ? 'success' : 'danger'" placement="top">
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
      <el-button type="primary" @click="$router.push(`/paper/edit/${id}`)">修改后重新提交</el-button>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPaperDetail } from '@/api/paper'
import { getPaperAuditLogs } from '@/api/audit'
import AttachmentDisplay from '@/components/AttachmentDisplay.vue'

const route = useRoute()
const id = route.params.id
const detail = ref(null)
const auditLogs = ref([])

const contextLabel = computed(() => {
  if (!detail.value) return ''
  return ['论文', detail.value.title, detail.value.applicantName].filter(Boolean).join('_')
})

onMounted(async () => {
  const res = await getPaperDetail(id)
  if (res.code === 200) detail.value = res.data
  const logsRes = await getPaperAuditLogs(id)
  if (logsRes.code === 200) auditLogs.value = logsRes.data
})

function statusType(s) {
  const m = { PENDING: 'warning', PENDING_LEADER: 'warning', APPROVED: 'success', REJECTED: 'danger', ARCHIVED: '' }
  return m[s] || 'info'
}
function statusLabel(s) {
  const m = { PENDING: '待审核', PENDING_LEADER: '领导审核中', APPROVED: '已通过', REJECTED: '已驳回', ARCHIVED: '已归档' }
  return m[s] || s
}
</script>
