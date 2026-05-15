<template>
  <div>
    <div class="page-header">
      <h2>论文成果</h2>
      <div class="header-actions">
        <el-button @click="handleExport" :loading="exportLoading">导出 Excel</el-button>
        <el-button type="primary" @click="$router.push('/paper/submit')">提交论文</el-button>
      </div>
    </div>

    <div class="card-wrapper">
      <el-card>
      <el-form :inline="true" :model="filters" size="default">
        <el-form-item label="期刊级别">
          <el-select v-model="filters.journalLevel" placeholder="全部" clearable style="width:150px">
            <el-option label="SCI一区" value="SCI一区" />
            <el-option label="SCI二区" value="SCI二区" />
            <el-option label="SCI三区" value="SCI三区" />
            <el-option label="SCI四区" value="SCI四区" />
            <el-option label="EI期刊" value="EI期刊" />
            <el-option label="CCF A类会议" value="CCF A类会议" />
            <el-option label="CCF B类会议" value="CCF B类会议" />
            <el-option label="CCF C类会议" value="CCF C类会议" />
            <el-option label="EI会议" value="EI会议" />
            <el-option label="北大核心期刊" value="北大核心期刊" />
            <el-option label="省级期刊" value="省级期刊" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width:140px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="领导审核中" value="PENDING_LEADER" />
            <el-option label="已归档" value="ARCHIVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
      </el-card>
    </div>

    <el-card>
      <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="title" label="论文标题" min-width="200" />
      <el-table-column prop="journalLevel" label="期刊级别" width="120" />
      <el-table-column prop="journalName" label="期刊/会议名称" min-width="150" />
      <el-table-column prop="applicantName" label="申报人" width="120" />
      <el-table-column prop="acceptanceDate" label="录用日期" width="120" />
      <el-table-column prop="status" label="审核状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button type="primary" link @click="$router.push(`/paper/detail/${row.id}`)">详情</el-button>
            <el-button v-if="isSecretary && row.status === 'PENDING'" type="warning" link @click="handleAudit(row)">审核</el-button>
            <el-button v-if="isLeader && row.status === 'PENDING_LEADER'" type="warning" link @click="handleAudit(row)">审核</el-button>
            <el-button type="warning" link :disabled="row.status !== 'REJECTED'" @click="$router.push(`/paper/edit/${row.id}`)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="fetchData" />
      </div>
    </el-card>

    <AuditDialog
      v-model:visible="auditVisible"
      title="审核论文"
      :approve-fn="approveFn"
      :reject-fn="rejectFn"
      @success="fetchData"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPaperList, deletePaper, exportPaper } from '@/api/paper'
import { secretaryApprovePaper, secretaryRejectPaper, leaderApprovePaper, leaderRejectPaper } from '@/api/audit'
import AuditDialog from '@/components/AuditDialog.vue'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const exportLoading = ref(false)
const filters = reactive({ journalLevel: '', status: '' })

const userInfo = computed(() => {
  try { return JSON.parse(localStorage.getItem('userInfo') || '{}') } catch { return {} }
})
const isSecretary = computed(() => userInfo.value.role === 'SECRETARY')
const isLeader = computed(() => userInfo.value.role === 'LEADER')

const auditVisible = ref(false)
const auditRow = ref(null)

const approveFn = computed(() => {
  if (!auditRow.value) return async () => {}
  const id = auditRow.value.id
  return isSecretary.value
    ? (data) => secretaryApprovePaper(id, data)
    : (data) => leaderApprovePaper(id, data)
})

const rejectFn = computed(() => {
  if (!auditRow.value) return async () => {}
  const id = auditRow.value.id
  return isSecretary.value
    ? (data) => secretaryRejectPaper(id, data)
    : (data) => leaderRejectPaper(id, data)
})

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.journalLevel) params.journalLevel = filters.journalLevel
    if (filters.status) params.status = filters.status
    const res = await getPaperList(params)
    if (res.code === 200) {
      list.value = res.data.content
      total.value = res.data.totalElements
    }
  } finally { loading.value = false }
}

function resetFilters() {
  filters.journalLevel = ''
  filters.status = ''
  page.value = 1
  fetchData()
}

function statusType(s) {
  const m = { PENDING: 'warning', PENDING_LEADER: 'warning', APPROVED: 'success', REJECTED: 'danger', ARCHIVED: '' }
  return m[s] || 'info'
}
function statusLabel(s) {
  const m = { PENDING: '待审核', PENDING_LEADER: '领导审核中', APPROVED: '已通过', REJECTED: '已驳回', ARCHIVED: '已归档' }
  return m[s] || s
}

function handleAudit(row) {
  auditRow.value = row
  auditVisible.value = true
}

async function handleExport() {
  exportLoading.value = true
  try {
    const params = {}
    if (filters.journalLevel) params.journalLevel = filters.journalLevel
    if (filters.status) params.status = filters.status
    const res = await exportPaper(params)
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '学术论文.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch { ElMessage.error('导出失败') }
  finally { exportLoading.value = false }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该论文记录吗？', '确认删除', { type: 'warning' })
    const res = await deletePaper(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch { }
}
</script>
