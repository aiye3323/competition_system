<template>
  <div class="page-container">
    <el-button text @click="$router.push('/software/list')">&lt; 返回列表</el-button>
    <h2>软件著作权详情</h2>
    <el-card class="card-wrapper">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="软件名称">{{ data.softwareName }}</el-descriptions-item>
        <el-descriptions-item label="所属单位">{{ data.affiliation }}</el-descriptions-item>
        <el-descriptions-item label="著作权人">{{ data.copyrightOwner }}</el-descriptions-item>
        <el-descriptions-item label="登记号">{{ data.registrationNumber }}</el-descriptions-item>
        <el-descriptions-item label="登记日期">{{ data.registrationDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(data.status)">{{ statusLabel(data.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申报人">{{ data.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ data.applyTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top:24px;">
        <h3>证明材料</h3>
        <AttachmentDisplay :file-urls="data.fileUrlList" :context-label="contextLabel" />
      </div>

      <div style="margin-top:24px;">
        <el-button v-if="data.status === 'REJECTED'" type="warning" @click="$router.push(`/software/edit/${data.id}`)">修改重提</el-button>
        <el-button @click="$router.push('/software/list')">返回列表</el-button>
      </div>
    </el-card>

    <el-card class="card-wrapper">
      <template #header><span style="font-weight:bold;">审核记录</span></template>
      <el-timeline v-if="auditLogs.length > 0">
        <el-timeline-item
          v-for="log in auditLogs"
          :key="log.id"
          :timestamp="log.auditTime"
          :color="log.result === 'APPROVED' ? '#67c23a' : '#f56c6c'"
        >
          <p><strong>{{ log.auditorName }}</strong>（{{ log.auditorRole === 'SECRETARY' ? '科研秘书' : '学院领导' }}）</p>
          <p>结果：{{ log.result === 'APPROVED' ? '通过' : '退回' }}</p>
          <p v-if="log.opinion">意见：{{ log.opinion }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审核记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getSoftwareDetail } from '@/api/software'
import { getSoftwareAuditLogs } from '@/api/audit'
import AttachmentDisplay from '@/components/AttachmentDisplay.vue'

const route = useRoute()
const data = ref({})
const auditLogs = ref([])

const contextLabel = computed(() => {
  if (!data.value) return ''
  return ['软著', data.value.softwareName, data.value.applicantName].filter(Boolean).join('_')
})

function statusType(s) {
  return { PENDING: 'warning', PENDING_LEADER: 'warning', ARCHIVED: 'success', REJECTED: 'danger' }[s] || 'info'
}
function statusLabel(s) {
  return { PENDING: '待审核', PENDING_LEADER: '领导审核中', ARCHIVED: '已归档', REJECTED: '已驳回' }[s] || s
}

onMounted(async () => {
  const res = await getSoftwareDetail(route.params.id)
  if (res.code === 200) data.value = res.data
  const logRes = await getSoftwareAuditLogs(route.params.id)
  if (logRes.code === 200) auditLogs.value = logRes.data
})
</script>
