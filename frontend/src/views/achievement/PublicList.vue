<template>
  <div>
    <div class="page-header">
      <h2>全院成果公开展示</h2>
      <el-button type="success" @click="handleExport">
        <el-icon><Download /></el-icon> 导出Excel
      </el-button>
    </div>

    <!-- 筛选区 -->
    <el-card style="margin-bottom:20px;">
      <el-form :inline="true" :model="filters">
        <el-form-item label="成果类型">
          <el-select v-model="filters.type" placeholder="全部类型" clearable style="width:140px;" @change="fetchData">
            <el-option label="全部" value="" />
            <el-option label="学科竞赛" value="COMPETITION" />
            <el-option label="创新项目" value="PROJECT" />
            <el-option label="学术论文" value="PAPER" />
            <el-option label="软件著作" value="SOFTWARE" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="filters.level1" placeholder="全部级别" clearable style="width:160px;" @change="fetchData">
            <el-option v-for="opt in levelOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="搜索名称" clearable style="width:180px;" />
        </el-form-item>
        <el-form-item label="年份">
          <el-select v-model="filters.year" placeholder="全部年份" clearable style="width:120px;" @change="fetchData">
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="String(y)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="list" v-loading="loading" stripe style="width:100%">
        <el-table-column label="成果类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)">{{ row.typeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="名称" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row)">
              {{ row.title }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申报人" width="100" />
        <el-table-column prop="applicantCollege" label="所在学院" width="140" show-overflow-tooltip />
        <el-table-column prop="level1" label="级别" width="120" />
        <el-table-column prop="level2" label="二级分类" width="120" />
        <el-table-column label="日期" width="130">
          <template #default="{ row }">
            {{ row.achievementDate || '-' }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Download } from '@element-plus/icons-vue'
import { getPublicAchievementList, exportAchievements } from '@/api/statistics'

const router = useRouter()

const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const filters = reactive({
  type: '',
  level1: '',
  keyword: '',
  year: ''
})

const competitionLevels = ['国家级A+', '国家级A', '国家级B', '国家级C', '国家级', '省级', '市级', '校级', '院级']
const projectLevels = ['国家级', '省级', '校级', '院级']
const paperLevels = ['SCI一区', 'SCI二区', 'SCI三区', 'SCI四区', 'EI期刊', 'EI会议', 'CCF A', 'CCF B', 'CCF C', '北大核心', '省级期刊']

const levelOptions = computed(() => {
  if (!filters.type || filters.type === 'COMPETITION') return competitionLevels
  if (filters.type === 'PROJECT') return projectLevels
  if (filters.type === 'PAPER') return paperLevels
  return [...competitionLevels, ...projectLevels, ...paperLevels]
})

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 10 }, (_, i) => currentYear - i)

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.type) params.type = filters.type
    if (filters.level1) params.level1 = filters.level1
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.year) params.year = filters.year

    const res = await getPublicAchievementList(params)
    if (res.code === 200) {
      list.value = res.data.content || []
      total.value = res.data.totalElements || 0
    }
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.type = ''
  filters.level1 = ''
  filters.keyword = ''
  filters.year = ''
  page.value = 1
  fetchData()
}

async function handleExport() {
  try {
    const params = {}
    if (filters.type) params.type = filters.type
    if (filters.level1) params.level1 = filters.level1
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.year) params.year = filters.year

    const blob = await exportAchievements(params)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '全院成果汇总.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    // error handled by interceptor
  }
}

function typeTag(type) {
  return { COMPETITION: '', PROJECT: 'success', PAPER: 'warning', SOFTWARE: 'info' }[type] || 'info'
}

function goDetail(row) {
  const map = { COMPETITION: '/competition/detail', PROJECT: '/project/detail', PAPER: '/paper/detail', SOFTWARE: '/software/detail' }
  router.push(map[row.type] + '/' + row.id)
}

onMounted(() => {
  fetchData()
})
</script>
