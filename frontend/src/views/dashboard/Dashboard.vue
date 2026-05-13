<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" style="border-top-color:#409eff;">
          <div class="stat-label">竞赛成果</div>
          <div class="stat-value" style="color:#409eff;">{{ stats.totalCompetitions }}</div>
          <el-icon :size="22" color="#409eff"><Trophy /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" style="border-top-color:#67c23a;">
          <div class="stat-label">创新项目</div>
          <div class="stat-value" style="color:#67c23a;">{{ stats.totalProjects }}</div>
          <el-icon :size="22" color="#67c23a"><Folder /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" style="border-top-color:#e6a23c;">
          <div class="stat-label">学术论文</div>
          <div class="stat-value" style="color:#e6a23c;">{{ stats.totalPapers }}</div>
          <el-icon :size="22" color="#e6a23c"><Document /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" style="border-top-color:#8b5cf6;">
          <div class="stat-label">软件著作</div>
          <div class="stat-value" style="color:#8b5cf6;">{{ stats.totalSoftware }}</div>
          <el-icon :size="22" color="#8b5cf6"><Stamp /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card" style="border-top-color:#f56c6c;">
          <div class="stat-label">成果总计</div>
          <div class="stat-value" style="color:#f56c6c;">{{ stats.totalAchievements }}</div>
          <el-icon :size="22" color="#f56c6c"><DataAnalysis /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行1：饼图 + 柱状图 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="chart-title">成果类型分布</span></template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="chart-title">年度成果趋势</span></template>
          <div ref="yearChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行2：学院分布 + 级别分布 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="chart-title">学院成果分布</span></template>
          <div ref="collegeChartRef" class="chart-container-sm"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="chart-title">竞赛获奖级别 / 项目期刊级别</span></template>
          <div ref="levelChartRef" class="chart-container-sm"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近成果 -->
    <el-card shadow="hover">
      <template #header><span class="chart-title">最近归档成果</span></template>
      <el-table :data="stats.recentItems" stripe style="width:100%;">
        <el-table-column prop="typeLabel" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)">{{ row.typeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="名称" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="申报人" width="120" />
        <el-table-column prop="level" label="级别" width="120" />
        <el-table-column prop="date" label="日期" width="130" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Trophy, Folder, Document, DataAnalysis, Stamp } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDashboardStats } from '@/api/statistics'

const router = useRouter()

const stats = ref({
  totalCompetitions: 0, totalProjects: 0, totalPapers: 0, totalSoftware: 0, totalAchievements: 0,
  byType: [], byYear: [], byAwardLevel: {}, byProjectLevel: {}, byJournalLevel: {}, byCollege: {},
  recentItems: []
})

const pieChartRef = ref(null)
const yearChartRef = ref(null)
const collegeChartRef = ref(null)
const levelChartRef = ref(null)

let pieChart = null, yearChart = null, collegeChart = null, levelChart = null

function initCharts() {
  if (!stats.value.totalAchievements && !stats.value.byType.length) return
  initPieChart()
  initYearChart()
  initCollegeChart()
  initLevelChart()
}

function initPieChart() {
  if (!pieChartRef.value) return
  pieChart?.dispose()
  pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['45%', '70%'], center: ['50%', '50%'],
      label: { show: true, formatter: '{b}\n{d}%' },
      data: (stats.value.byType || []).map(t => ({ name: t.label, value: t.count })),
      itemStyle: { color: params => ['#409eff', '#67c23a', '#e6a23c', '#8b5cf6'][params.dataIndex] }
    }]
  })
}

function initYearChart() {
  if (!yearChartRef.value) return
  yearChart?.dispose()
  yearChart = echarts.init(yearChartRef.value)
  const years = (stats.value.byYear || []).map(y => String(y.year))
  yearChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: 50, right: 20, bottom: 30, top: 20 },
    xAxis: { type: 'category', data: years },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '竞赛', type: 'bar', data: (stats.value.byYear || []).map(y => y.competitions), itemStyle: { color: '#409eff' } },
      { name: '项目', type: 'bar', data: (stats.value.byYear || []).map(y => y.projects), itemStyle: { color: '#67c23a' } },
      { name: '论文', type: 'bar', data: (stats.value.byYear || []).map(y => y.papers), itemStyle: { color: '#e6a23c' } },
      { name: '软著', type: 'bar', data: (stats.value.byYear || []).map(y => y.software || 0), itemStyle: { color: '#8b5cf6' } }
    ]
  })
}

function initCollegeChart() {
  if (!collegeChartRef.value) return
  collegeChart?.dispose()
  collegeChart = echarts.init(collegeChartRef.value)
  const keys = Object.keys(stats.value.byCollege || {})
  const values = Object.values(stats.value.byCollege || {})
  if (!keys.length) {
    collegeChart.setOption({ title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } } })
    return
  }
  collegeChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 150, right: 30, bottom: 20, top: 20 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: keys },
    series: [{ type: 'bar', data: values, itemStyle: { color: '#409eff' }, label: { show: true, position: 'right' } }]
  })
}

function initLevelChart() {
  if (!levelChartRef.value) return
  levelChart?.dispose()
  levelChart = echarts.init(levelChartRef.value)
  const awardKeys = Object.keys(stats.value.byAwardLevel || {})
  const awardValues = Object.values(stats.value.byAwardLevel || {})
  const projKeys = Object.keys(stats.value.byProjectLevel || {})
  const projValues = Object.values(stats.value.byProjectLevel || {})
  const journalKeys = Object.keys(stats.value.byJournalLevel || {})
  const journalValues = Object.values(stats.value.byJournalLevel || {})

  if (!awardKeys.length && !projKeys.length && !journalKeys.length) {
    levelChart.setOption({ title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } } })
    return
  }

  const categories = []
  const awardData = []
  const projData = []
  const paperData = []
  const allKeys = [...new Set([...awardKeys, ...projKeys, ...journalKeys])]
  for (const k of allKeys) {
    categories.push(k)
    awardData.push(stats.value.byAwardLevel?.[k] || 0)
    projData.push(stats.value.byProjectLevel?.[k] || 0)
    paperData.push(stats.value.byJournalLevel?.[k] || 0)
  }

  levelChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, data: ['竞赛获奖', '创新项目', '学术论文'] },
    grid: { left: 150, right: 30, bottom: 30, top: 20 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: categories },
    series: [
      { name: '竞赛获奖', type: 'bar', data: awardData, itemStyle: { color: '#409eff' }, label: { show: true, position: 'right' } },
      { name: '创新项目', type: 'bar', data: projData, itemStyle: { color: '#67c23a' }, label: { show: true, position: 'right' } },
      { name: '学术论文', type: 'bar', data: paperData, itemStyle: { color: '#e6a23c' }, label: { show: true, position: 'right' } }
    ]
  })
}

function handleResize() {
  pieChart?.resize(); yearChart?.resize(); collegeChart?.resize(); levelChart?.resize()
}

function typeTag(type) {
  return { COMPETITION: '', PROJECT: 'success', PAPER: 'warning', SOFTWARE: 'info' }[type] || 'info'
}

function goDetail(row) {
  const map = { COMPETITION: '/competition/detail', PROJECT: '/project/detail', PAPER: '/paper/detail', SOFTWARE: '/software/detail' }
  router.push(map[row.type] + '/' + row.id)
}

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    if (res.code === 200) { stats.value = res.data; await nextTick(); initCharts() }
  } catch (e) { /* handled */ }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose(); yearChart?.dispose(); collegeChart?.dispose(); levelChart?.dispose()
})
</script>

<style scoped>
.stat-row {
  margin-bottom: 20px;
}
.stat-card {
  text-align: center;
  border-top: 4px solid;
}
.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}
.stat-value {
  font-size: 30px;
  font-weight: bold;
  margin: 8px 0;
}
.chart-title {
  font-weight: bold;
  color: var(--text-primary);
}
.chart-container {
  width: 100%;
  height: 350px;
}
.chart-container-sm {
  width: 100%;
  height: 300px;
}
.chart-row {
  margin-bottom: 20px;
}
</style>
