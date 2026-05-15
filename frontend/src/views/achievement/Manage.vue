<template>
  <div class="manage-page">
    <div class="page-header">
      <h2>成果管理</h2>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="学科竞赛" name="competition">
        <CompetitionList />
      </el-tab-pane>
      <el-tab-pane label="创新项目" name="project">
        <ProjectList />
      </el-tab-pane>
      <el-tab-pane label="软件著作权" name="software">
        <SoftwareList />
      </el-tab-pane>
      <el-tab-pane label="学术论文" name="paper">
        <PaperList />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CompetitionList from '@/views/competition/List.vue'
import ProjectList from '@/views/project/List.vue'
import SoftwareList from '@/views/software/List.vue'
import PaperList from '@/views/paper/List.vue'

const route = useRoute()
const router = useRouter()

const activeTab = ref(route.query.tab || 'competition')

function handleTabChange(name) {
  activeTab.value = name
  router.replace({ query: { tab: name } })
}

watch(() => route.query.tab, (val) => {
  if (val && val !== activeTab.value) {
    activeTab.value = val
  }
})
</script>

<style scoped>
.manage-page {
  padding: 0;
}

.page-header {
  margin-bottom: var(--space-md);
}

.page-header h2 {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

:deep(.el-tabs__header) {
  margin-bottom: var(--space-lg);
}

:deep(.el-tabs__item) {
  font-size: 15px;
  padding: 0 24px;
  height: 44px;
  line-height: 44px;
  color: var(--text-regular);
}

:deep(.el-tabs__item.is-active) {
  color: var(--primary-color);
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  height: 3px;
  background-color: var(--primary-color);
}
</style>
