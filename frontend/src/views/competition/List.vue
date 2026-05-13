<template>
  <div>
    <div class="page-header">
      <h2>学科竞赛成果</h2>
      <div>
        <el-button @click="handleExport" :loading="exportLoading">导出Excel</el-button>
        <el-button type="primary" @click="$router.push('/competition/submit')">新建竞赛</el-button>
      </div>
    </div>

    <el-card style="margin-bottom:20px;">
      <el-form :inline="true" :model="filters" size="default">
        <el-form-item label="竞赛类别">
          <el-select v-model="filters.category" placeholder="全部" clearable style="width:140px">
            <el-option label="A类" value="A类" />
            <el-option label="B类" value="B类" />
            <el-option label="C类" value="C类" />
          </el-select>
        </el-form-item>
        <el-form-item label="获奖级别">
          <el-select v-model="filters.awardLevel" placeholder="全部" clearable style="width:140px">
            <el-option label="国家级" value="国家级" />
            <el-option label="省级" value="省级" />
            <el-option label="市级" value="市级" />
            <el-option label="校级" value="校级" />
            <el-option label="院级" value="院级" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width:140px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="领导审核中" value="PENDING_LEADER" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column prop="competitionName" label="竞赛名称" min-width="180" />
      <el-table-column prop="category" label="类别" width="80" />
      <el-table-column prop="awardLevel" label="获奖级别" width="100" />
      <el-table-column prop="awardGrade" label="获奖等级" width="100" />
      <el-table-column prop="awardDate" label="获奖时间" width="120" />
      <el-table-column prop="status" label="审核状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button type="primary" link @click="$router.push(`/competition/detail/${row.id}`)">详情</el-button>
            <el-button type="warning" link :disabled="row.status !== 'DRAFT' && row.status !== 'REJECTED'"
              @click="$router.push(`/competition/edit/${row.id}`)">编辑</el-button>
            <el-button type="danger" link :disabled="row.status !== 'DRAFT'"
              @click="handleDelete(row.id)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="fetchData" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCompetitionList, deleteCompetition, exportCompetition } from '@/api/competition'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const exportLoading = ref(false)

const filters = reactive({
  category: '',
  awardLevel: '',
  status: ''
})

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.category) params.category = filters.category
    if (filters.awardLevel) params.awardLevel = filters.awardLevel
    if (filters.status) params.status = filters.status

    const res = await getCompetitionList(params)
    if (res.code === 200) {
      list.value = res.data.content
      total.value = res.data.totalElements
    }
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.category = ''
  filters.awardLevel = ''
  filters.status = ''
  page.value = 1
  fetchData()
}

function statusType(status) {
  const map = { DRAFT: 'info', PENDING: 'warning', PENDING_LEADER: "warning", FIRST_REVIEW: "", FINAL_REVIEW: 'primary', APPROVED: 'success', REJECTED: 'danger', ARCHIVED: '' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { DRAFT: '草稿', PENDING: '待审核', PENDING_LEADER: "领导审核中", FIRST_REVIEW: "一审中", FINAL_REVIEW: '终审中', APPROVED: '已通过', REJECTED: '已驳回', ARCHIVED: '已归档' }
  return map[status] || status
}

async function handleExport() {
  exportLoading.value = true
  try {
    const params = {}
    if (filters.category) params.category = filters.category
    if (filters.awardLevel) params.awardLevel = filters.awardLevel
    if (filters.status) params.status = filters.status
    const res = await exportCompetition(params)
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '学科竞赛.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch { ElMessage.error('导出失败') }
  finally { exportLoading.value = false }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该竞赛记录吗？', '确认删除', { type: 'warning' })
    const res = await deleteCompetition(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch { }
}
</script>
