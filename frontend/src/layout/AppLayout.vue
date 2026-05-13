<template>
  <div class="app-layout">
    <!-- 顶部导航栏 -->
    <header class="top-nav">
      <div class="logo">🏆 科研竞赛管理系统</div>
      <el-menu
        mode="horizontal"
        :default-active="route.path"
        router
        :ellipsis="false"
        class="nav-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/achievement/public">
          <el-icon><View /></el-icon>
          <span>全院成果</span>
        </el-menu-item>
        <el-menu-item index="/submit">
          <el-icon><Plus /></el-icon>
          <span>提交成果</span>
        </el-menu-item>
        <el-sub-menu index="achievements">
          <template #title>
            <el-icon><Trophy /></el-icon>
            <span>成果管理</span>
          </template>
          <el-menu-item index="/competition/list">
            <el-icon><Trophy /></el-icon>
            <span>竞赛成果</span>
          </el-menu-item>
          <el-menu-item index="/project/list">
            <el-icon><Folder /></el-icon>
            <span>创新项目</span>
          </el-menu-item>
          <el-menu-item index="/paper/list">
            <el-icon><Document /></el-icon>
            <span>论文成果</span>
          </el-menu-item>
          <el-menu-item index="/software/list">
            <el-icon><Stamp /></el-icon>
            <span>软件著作</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-if="hasRole('SECRETARY') || hasRole('LEADER')" index="/audit/list">
          <el-icon><Edit /></el-icon>
          <span>审核管理</span>
        </el-menu-item>
        <el-sub-menu v-if="hasRole('ADMIN')" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/logs">
            <el-icon><Monitor /></el-icon>
            <span>操作日志</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
      <div class="nav-right">
        <NotificationBell />
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            {{ userInfo?.realName || userInfo?.username || '用户' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown, Trophy, Plus, Edit, Folder, Monitor,
  Document, DataAnalysis, View, Stamp, User, Setting
} from '@element-plus/icons-vue'
import NotificationBell from '@/components/NotificationBell.vue'

const route = useRoute()
const router = useRouter()

const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

function hasRole(role) {
  return userInfo.value.role === role
}

function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    router.push('/login')
    ElMessage.success('已退出登录')
  }
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--app-bg);
}

/* ========== 顶部导航 ========== */
.top-nav {
  height: var(--nav-height);
  display: flex;
  align-items: center;
  background: var(--nav-bg);
  border-bottom: 1px solid var(--border-color);
  padding: 0 16px;
  flex-shrink: 0;
  transition: background var(--transition-speed), border-color var(--transition-speed);
  position: relative;
  z-index: 100;
}

.logo {
  font-size: 16px;
  font-weight: bold;
  color: var(--text-primary);
  white-space: nowrap;
  margin-right: 24px;
  padding-right: 24px;
  border-right: 1px solid var(--border-color);
}

.nav-menu {
  flex: 1;
  border-bottom: none !important;
  height: var(--nav-height);
  background: transparent;
  overflow: hidden;
}

.nav-menu .el-menu-item,
.nav-menu .el-sub-menu .el-sub-menu__title {
  height: var(--nav-height) !important;
  line-height: var(--nav-height) !important;
  color: var(--text-secondary) !important;
}

.nav-menu .el-menu-item.is-active {
  color: var(--primary-color) !important;
  border-bottom-color: var(--primary-color) !important;
}

.nav-menu .el-menu-item:hover,
.nav-menu .el-sub-menu .el-sub-menu__title:hover {
  background: var(--hover-bg) !important;
  color: var(--text-primary) !important;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 16px;
  border-left: 1px solid var(--border-color);
  flex-shrink: 0;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-primary);
  font-size: 14px;
  padding: 0 8px;
}

/* ========== 主内容区 ========== */
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  max-width: var(--content-max-width);
  width: 100%;
  margin: 0 auto;
}
</style>
