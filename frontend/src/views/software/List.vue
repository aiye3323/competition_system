<template>
  <div>
    <div class="page-header">
      <h2>软件著作权</h2>
      <div class="header-actions">
        <el-button @click="handleExport" :loading="exportLoading">导出 Excel</el-button>
        <el-button type="primary" @click="$router.push('/software/submit')">提交软著</el-button>
      </div>
    </div>

    <div class="card-wrapper">
      <el-card>
      <el-form :inline="true" :model="filters">
        <el-form-item label="登记号">
          <el-input v-model="filters.registrationNumber" placeholder="搜索登记号" clearable style="width:200px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width:140px;">
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
      <el-table :data="list" v-loading="loading" stripe style="width:100%">
        <el-table-column prop="softwareName" label="软件名称" show-overflow-tooltip />
        <el-table-column prop="registrationNumber" label="登记号" width="160" />
        <el-table-column prop="affiliation" label="所属单位" width="100" />
        <el-table-column prop="copyrightOwner" label="著作权人" width="120" show-overflow-tooltip />
        <el-table-column prop="registrationDate" label="登记日期" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申报人" width="100" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link @click="$router.push(`/software/detail/${row.id}`)">详情</el-button>
              <el-button v-if="canAudit(row)" type="warning" link @click="handleAudit(row)">审核</el-button>
              <el-button v-if="row.status === 'REJECTED'" type="warning" link @click="$router.push(`/software/edit/${row.id}`)">修改</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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
      title="审核软件著作权"
      :approve-fn="approveFn"
      :reject-fn="rejectFn"
      @success="fetchData"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSoftwareList, deleteSoftware, exportSoftware } from '@/api/software'
import { secretaryApproveSoftware, secretaryRejectSoftware, leaderApproveSoftware, leaderRejectSoftware } from '@/api/audit'
import AuditDialog from '@/components/AuditDialog.vue'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const exportLoading = ref(false)

const filters = reactive({ registrationNumber: '', status: '' })

const userRole = computed(() => {
  try { return JSON.parse(localStorage.getItem('userInfo') || '{}').role || 'STUDENT' }
  catch { return 'STUDENT' }
})

const auditVisible = ref(false)
const auditRow = ref(null)

function canAudit(row) {
  return (userRole.value === 'SECRETARY' && row.status === 'PENDING') ||
         (userRole.value === 'LEADER' && row.status === 'PENDING_LEADER')
}

const approveFn = computed(() => {
  if (!auditRow.value) return async () => {}
  const id = auditRow.value.id
  return userRole.value === 'SECRETARY'
    ? (data) => secretaryApproveSoftware(id, data)
    : (data) => leaderApproveSoftware(id, data)
})

const rejectFn = computed(() => {
  if (!auditRow.value) return async () => {}
  const id = auditRow.value.id
  return userRole.value === 'SECRETARY'
    ? (data) => secretaryRejectSoftware(id, data)
    : (data) => leaderRejectSoftware(id, data)
})

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.registrationNumber) params.registrationNumber = filters.registrationNumber
    if (filters.status) params.status = filters.status
    const res = await getSoftwareList(params)
    if (res.code === 200) {
      list.value = res.data.content
      total.value = res.data.totalElements
    }
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.registrationNumber = ''
  filters.status = ''
  page.value = 1
  fetchData()
}

function handleAudit(row) {
  auditRow.value = row
  auditVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该记录？', '提示', { type: 'warning' })
  try {
    const res = await deleteSoftware(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch { /* cancel */ }
}

async function handleExport() {
  exportLoading.value = true
  try {
    const params = {}
    if (filters.registrationNumber) params.registrationNumber = filters.registrationNumber
    if (filters.status) params.status = filters.status
    const res = await exportSoftware(params)
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '软件著作权.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch { ElMessage.error('导出失败') }
  finally { exportLoading.value = false }
}

function statusType(s) {
  return { PENDING: 'warning', PENDING_LEADER: 'warning', ARCHIVED: 'success', REJECTED: 'danger' }[s] || 'info'
}
function statusLabel(s) {
  return { PENDING: '待审核', PENDING_LEADER: '领导审核中', ARCHIVED: '已归档', REJECTED: '已驳回' }[s] || s
}

onMounted(() => fetchData())
</script>
