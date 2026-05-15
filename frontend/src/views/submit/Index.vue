<template>
  <div>
    <div class="page-header">
      <h2>提交成果</h2>
      <p class="header-desc">请选择要提交的科研成果类型</p>
    </div>

    <div class="type-grid">
      <button class="type-card" @click="$router.push('/competition/submit')">
        <div class="type-icon type-icon--indigo">
          <el-icon :size="32"><Trophy /></el-icon>
        </div>
        <span class="type-title">学科竞赛</span>
        <span class="type-desc">提交竞赛获奖成果</span>
      </button>

      <button class="type-card" @click="$router.push('/project/submit')">
        <div class="type-icon type-icon--green">
          <el-icon :size="32"><Folder /></el-icon>
        </div>
        <span class="type-title">创新项目</span>
        <span class="type-desc">提交大学生创新训练项目</span>
      </button>

      <button class="type-card" @click="$router.push('/paper/submit')">
        <div class="type-icon type-icon--amber">
          <el-icon :size="32"><Document /></el-icon>
        </div>
        <span class="type-title">学术论文</span>
        <span class="type-desc">提交已发表的学术论文</span>
      </button>

      <button class="type-card" @click="$router.push('/software/submit')">
        <div class="type-icon type-icon--purple">
          <el-icon :size="32"><Stamp /></el-icon>
        </div>
        <span class="type-title">软件著作</span>
        <span class="type-desc">提交软件著作权登记</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { Trophy, Folder, Document, Stamp } from '@element-plus/icons-vue'
</script>

<style scoped>
.page-header {
  margin-bottom: var(--space-xl);
}

.page-header h2 {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.header-desc {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-lg);
}

.type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-2xl) var(--space-lg);
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--card-radius);
  cursor: pointer;
  transition: box-shadow var(--transition-normal), border-color var(--transition-fast);
  font-family: inherit;
}

.type-card:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-md);
}

.type-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.type-icon--indigo { background: var(--primary-light); color: var(--primary-color); }
.type-icon--green  { background: var(--success-light); color: var(--success-color); }
.type-icon--amber  { background: var(--warning-light); color: var(--warning-color); }
.type-icon--purple { background: #F5F3FF; color: #7C3AED; }

.type-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
}

.type-desc {
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

@media (max-width: 720px) {
  .type-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
