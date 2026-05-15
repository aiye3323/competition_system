<template>
  <div>
    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-item">
        <div class="stat-icon stat-icon--blue">
          <el-icon :size="22"><Trophy /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.totalCompetitions }}</div>
          <div class="stat-label">竞赛成果</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon--green">
          <el-icon :size="22"><Folder /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.totalProjects }}</div>
          <div class="stat-label">创新项目</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon--amber">
          <el-icon :size="22"><Document /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.totalPapers }}</div>
          <div class="stat-label">学术论文</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon--purple">
          <el-icon :size="22"><Stamp /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.totalSoftware }}</div>
          <div class="stat-label">软件著作</div>
        </div>
      </div>
      <div class="stat-item stat-item--total">
        <div class="stat-total-value">{{ stats.totalAchievements }}</div>
        <div class="stat-total-label">成果总计</div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <div class="quick-action-card" @click="$router.push('/submit')">
        <el-icon :size="28"><Plus /></el-icon>
        <span>提交成果</span>
      </div>
      <div class="quick-action-card" @click="$router.push('/competition/list')">
        <el-icon :size="28"><Trophy /></el-icon>
        <span>竞赛成果</span>
      </div>
      <div class="quick-action-card" @click="$router.push('/project/list')">
        <el-icon :size="28"><Folder /></el-icon>
        <span>创新项目</span>
      </div>
      <div class="quick-action-card" @click="$router.push('/paper/list')">
        <el-icon :size="28"><Document /></el-icon>
        <span>学术论文</span>
      </div>
      <div class="quick-action-card" @click="$router.push('/software/list')">
        <el-icon :size="28"><Stamp /></el-icon>
        <span>软件著作</span>
      </div>
      <div class="quick-action-card" @click="$router.push('/achievement/public')">
        <el-icon :size="28"><View /></el-icon>
        <span>全院成果</span>
      </div>
    </div>

    <!-- 图表行 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <div class="chart-card">
          <h3 class="chart-title">成果类型分布</h3>
          <div ref="pieChartRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <h3 class="chart-title">年度成果趋势</h3>
          <div ref="yearChartRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <div class="chart-card">
          <h3 class="chart-title">学院成果分布</h3>
          <div ref="collegeChartRef" class="chart-box-sm"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <h3 class="chart-title">获奖 / 项目 / 期刊级别</h3>
          <div ref="levelChartRef" class="chart-box-sm"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 最近成果 -->
    <div class="chart-card">
      <h3 class="chart-title">最近归档成果</h3>
      <el-table :data="stats.recentItems" stripe style="width:100%">
        <el-table-column prop="typeLabel" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small" effect="plain">{{ row.typeLabel }}</el-tag>
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
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Trophy, Folder, Document, Stamp } from '@element-plus/icons-vue'
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

const CHART_COLORS = ['#4C51BF', '#059669', '#D97706', '#7C3AED']

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
    legend: { bottom: 0, textStyle: { color: '#4B5563' } },
    series: [{
      type: 'pie', radius: ['48%', '72%'], center: ['50%', '48%'],
      label: { show: true, formatter: '{b}\n{d}%', color: '#4B5563', fontSize: 12 },
      data: (stats.value.byType || []).map(t => ({ name: t.label, value: t.count })),
      itemStyle: { color: params => CHART_COLORS[params.dataIndex] }
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
    legend: { bottom: 0, textStyle: { color: '#4B5563' } },
    grid: { left: 48, right: 16, bottom: 32, top: 16 },
    xAxis: { type: 'category', data: years, axisLine: { lineStyle: { color: '#E8E4DF' } }, axisLabel: { color: '#4B5563' } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#F0EDE9' } }, axisLabel: { color: '#9CA3AF' } },
    series: [
      { name: '竞赛', type: 'bar', data: (stats.value.byYear || []).map(y => y.competitions), itemStyle: { color: CHART_COLORS[0], borderRadius: [4, 4, 0, 0] }, barMaxWidth: 32 },
      { name: '项目', type: 'bar', data: (stats.value.byYear || []).map(y => y.projects), itemStyle: { color: CHART_COLORS[1], borderRadius: [4, 4, 0, 0] }, barMaxWidth: 32 },
      { name: '论文', type: 'bar', data: (stats.value.byYear || []).map(y => y.papers), itemStyle: { color: CHART_COLORS[2], borderRadius: [4, 4, 0, 0] }, barMaxWidth: 32 },
      { name: '软著', type: 'bar', data: (stats.value.byYear || []).map(y => y.software || 0), itemStyle: { color: CHART_COLORS[3], borderRadius: [4, 4, 0, 0] }, barMaxWidth: 32 }
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
    collegeChart.setOption({ title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#9CA3AF', fontSize: 14 } } })
    return
  }
  collegeChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 140, right: 40, bottom: 16, top: 16 },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#F0EDE9' } }, axisLabel: { color: '#9CA3AF' } },
    yAxis: { type: 'category', data: keys, axisLine: { lineStyle: { color: '#E8E4DF' } }, axisLabel: { color: '#4B5563' } },
    series: [{ type: 'bar', data: values, itemStyle: { color: '#4C51BF', borderRadius: [0, 4, 4, 0] }, barMaxWidth: 20, label: { show: true, position: 'right', color: '#4B5563' } }]
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
    levelChart.setOption({ title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#9CA3AF', fontSize: 14 } } })
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
    legend: { bottom: 0, textStyle: { color: '#4B5563' }, data: ['竞赛获奖', '创新项目', '学术论文'] },
    grid: { left: 140, right: 40, bottom: 32, top: 16 },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#F0EDE9' } }, axisLabel: { color: '#9CA3AF' } },
    yAxis: { type: 'category', data: categories, axisLine: { lineStyle: { color: '#E8E4DF' } }, axisLabel: { color: '#4B5563' } },
    series: [
      { name: '竞赛获奖', type: 'bar', data: awardData, itemStyle: { color: CHART_COLORS[0], borderRadius: [0, 4, 4, 0] }, barMaxWidth: 16, label: { show: true, position: 'right', color: '#4B5563', fontSize: 11 } },
      { name: '创新项目', type: 'bar', data: projData, itemStyle: { color: CHART_COLORS[1], borderRadius: [0, 4, 4, 0] }, barMaxWidth: 16, label: { show: true, position: 'right', color: '#4B5563', fontSize: 11 } },
      { name: '学术论文', type: 'bar', data: paperData, itemStyle: { color: CHART_COLORS[2], borderRadius: [0, 4, 4, 0] }, barMaxWidth: 16, label: { show: true, position: 'right', color: '#4B5563', fontSize: 11 } }
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
/* 快捷操作 */
.quick-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.quick-action-card {
  flex: 1;
  min-width: 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--card-radius);
  cursor: pointer;
  color: var(--primary-color);
  transition: all var(--transition-fast);
}

.quick-action-card:hover {
  background: var(--primary-light);
  border-color: var(--primary-color);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.quick-action-card span {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-regular);
  white-space: nowrap;
}

/* 统计卡片网格 */
.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr 1.2fr;
  gap: 16px;
  margin-bottom: 24px;
}

.stat-item {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--card-radius);
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: box-shadow var(--transition-normal);
}

.stat-item:hover {
  box-shadow: var(--shadow-md);
}

.stat-item--total {
  background: var(--primary-color);
  border-color: var(--primary-color);
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  justify-content: center;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon--blue   { background: #EEF2FF; color: #4C51BF; }
.stat-icon--green  { background: #ECFDF5; color: #059669; }
.stat-icon--amber  { background: #FFFBEB; color: #D97706; }
.stat-icon--purple { background: #F5F3FF; color: #7C3AED; }

.stat-body {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
  font-weight: 500;
}

.stat-total-value {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.stat-total-label {
  font-size: 13px;
  color: rgba(255,255,255,0.75);
  font-weight: 500;
}

/* 图表卡片 */
.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--card-radius);
  padding: var(--card-padding);
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px;
}

.chart-box {
  width: 100%;
  height: 340px;
}

.chart-box-sm {
  width: 100%;
  height: 290px;
}
</style>
