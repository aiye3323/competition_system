<template>
  <div>
    <h2>审核管理</h2>

    <el-card style="margin-bottom:20px;">
      <el-form :inline="true" :model="filters" size="default">
        <el-form-item label="审核状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width:140px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="一审中" value="FIRST_REVIEW" />
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
      <el-table-column prop="applicantName" label="申请人" width="120" />
      <el-table-column prop="applyTime" label="申请时间" width="160" />
      <el-table-column prop="status" label="审核状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="$router.push(`/review/detail/${row.id}`)">审核</el-button>
          <el-button type="primary" link @click="$router.push(`/competition/detail/${row.id}`)">详情</el-button>
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
import { ref, reactive, onMounted, computed } from 'vue'
import { getCompetitionList } from '@/api/competition'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

const filters = reactive({
  status: ''
})

onMounted(() => {
  // 默认显示当前角色应该审核的状态
  if (userInfo.value.role === 'SECRETARY') {
    filters.status = 'PENDING'
  } else if (userInfo.value.role === 'LEADER') {
    filters.status = 'FIRST_REVIEW'
  }
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
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
  filters.status = ''
  page.value = 1
  fetchData()
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
