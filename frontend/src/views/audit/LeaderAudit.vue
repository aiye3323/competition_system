<template>
  <div>
    <h2>领导审核</h2>

    <el-table :data="list" border stripe v-loading="loading" @row-click="handleRowClick"
              :row-class-name="rowClassName">
      <el-table-column prop="competitionName" label="竞赛名称" min-width="160" />
      <el-table-column prop="category" label="类别" width="70" />
      <el-table-column prop="applicantName" label="申报人" width="100" />
      <el-table-column prop="applyTime" label="申报时间" width="150" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag type="warning" size="small">领导审核中</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click.stop="showDetail(row)">审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" title="审核详情" width="700px" top="5vh">
      <template v-if="currentDetail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="竞赛名称" :span="2">{{ currentDetail.competitionName }}</el-descriptions-item>
          <el-descriptions-item label="竞赛类别">{{ currentDetail.category }}</el-descriptions-item>
          <el-descriptions-item label="获奖级别">{{ currentDetail.awardLevel }}</el-descriptions-item>
          <el-descriptions-item label="获奖等级">{{ currentDetail.awardGrade }}</el-descriptions-item>
          <el-descriptions-item label="获奖单位" :span="2">{{ currentDetail.awardUnit }}</el-descriptions-item>
          <el-descriptions-item label="申报人">{{ currentDetail.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="申报时间">{{ currentDetail.applyTime }}</el-descriptions-item>
          <el-descriptions-item label="指导教师" :span="2">{{ currentDetail.advisor }}</el-descriptions-item>
          <el-descriptions-item label="参赛选手" :span="2">{{ currentDetail.participants }}</el-descriptions-item>
        </el-descriptions>

        <el-timeline v-if="auditLogs.length > 0" style="margin-top:16px;">
          <h4 style="margin:12px 0;">审核记录</h4>
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
            <p v-if="log.opinion" class="text-regular" style="margin:8px 0 0;">意见：{{ log.opinion }}</p>
          </el-timeline-item>
        </el-timeline>

        <el-divider />
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-form-item label="审核意见" prop="opinion">
            <el-input v-model="form.opinion" type="textarea" :rows="3" placeholder="请输入审核意见（退回时必填）" />
          </el-form-item>
          <el-form-item>
            <el-button type="success" :loading="submitting" @click="handleApprove">通过</el-button>
            <el-button type="danger" :loading="submitting" @click="handleReject">退回</el-button>
          </el-form-item>
        </el-form>
      </template>
    </el-dialog>

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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingList, leaderApprove, leaderReject, getAuditLogs } from '@/api/audit'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const detailVisible = ref(false)
const currentDetail = ref(null)
const auditLogs = ref([])
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  opinion: ''
})

const rules = {
  opinion: [{
    validator: (rule, value, callback) => {
      if (submittingAction.value === 'reject' && !value) {
        callback(new Error('退回时必须填写审核意见'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }]
}

const submittingAction = ref('')

onMounted(() => {
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const params = { role: 'LEADER', page: page.value, size: size.value }
    const res = await getPendingList(params)
    if (res.code === 200) {
      list.value = res.data.content
      total.value = res.data.totalElements
    }
  } finally {
    loading.value = false
  }
}

function handleRowClick() {}

function rowClassName() {
  return 'cursor-pointer'
}

async function showDetail(row) {
  currentDetail.value = row
  auditLogs.value = []
  form.opinion = ''

  const logsRes = await getAuditLogs(row.id)
  if (logsRes.code === 200) {
    auditLogs.value = logsRes.data
  }

  detailVisible.value = true
}

async function handleApprove() {
  submittingAction.value = 'approve'
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await leaderApprove(currentDetail.value.id, { opinion: form.opinion })
    if (res.code === 200) {
      ElMessage.success('审核通过')
      detailVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

async function handleReject() {
  submittingAction.value = 'reject'
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await leaderReject(currentDetail.value.id, { opinion: form.opinion })
    if (res.code === 200) {
      ElMessage.success('已退回')
      detailVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}
</style>
