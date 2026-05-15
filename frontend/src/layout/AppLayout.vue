<template>
  <div class="app-layout">
    <header class="top-nav">
      <div class="nav-left">
        <router-link to="/dashboard" class="logo">
          <svg class="logo-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 10v6M2 10l10-5 10 5-10 5z"/>
            <path d="M6 12v5c0 1.1 2.7 2 6 2s6-.9 6-2v-5"/>
          </svg>
          <span class="logo-text">科研竞赛管理</span>
        </router-link>
        <nav class="nav-links">
          <router-link to="/dashboard" class="nav-item" active-class="active">
            <el-icon :size="18"><DataAnalysis /></el-icon>
            <span>数据概览</span>
          </router-link>
          <router-link to="/achievement/public" class="nav-item" active-class="active">
            <el-icon :size="18"><View /></el-icon>
            <span>全院成果</span>
          </router-link>
          <router-link to="/submit" class="nav-item" active-class="active">
            <el-icon :size="18"><Plus /></el-icon>
            <span>提交成果</span>
          </router-link>
          <el-dropdown trigger="hover" popper-class="nav-dropdown">
            <span class="nav-item" :class="{ active: isAchievementActive }">
              <el-icon :size="18"><Trophy /></el-icon>
              <span>成果管理</span>
              <el-icon :size="14" class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <router-link to="/competition/list" class="dropdown-link">竞赛成果</router-link>
                </el-dropdown-item>
                <el-dropdown-item>
                  <router-link to="/project/list" class="dropdown-link">创新项目</router-link>
                </el-dropdown-item>
                <el-dropdown-item>
                  <router-link to="/paper/list" class="dropdown-link">论文成果</router-link>
                </el-dropdown-item>
                <el-dropdown-item>
                  <router-link to="/software/list" class="dropdown-link">软件著作</router-link>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <router-link v-if="hasRole('SECRETARY') || hasRole('LEADER')" to="/audit/list" class="nav-item" active-class="active">
            <el-icon :size="18"><Edit /></el-icon>
            <span>审核管理</span>
          </router-link>
          <el-dropdown v-if="hasRole('ADMIN')" trigger="hover" popper-class="nav-dropdown">
            <span class="nav-item" :class="{ active: isSystemActive }">
              <el-icon :size="18"><Setting /></el-icon>
              <span>系统管理</span>
              <el-icon :size="14" class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <router-link to="/admin/users" class="dropdown-link">用户管理</router-link>
                </el-dropdown-item>
                <el-dropdown-item>
                  <router-link to="/admin/logs" class="dropdown-link">操作日志</router-link>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </nav>
      </div>
      <div class="nav-right">
        <NotificationBell />
        <el-dropdown @command="handleCommand" trigger="click">
          <div class="user-avatar">
            <span class="avatar-text">{{ avatarLetter }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <div class="dropdown-user-info">
                <span class="dropdown-name">{{ userInfo?.realName || userInfo?.username || '用户' }}</span>
                <span class="dropdown-role">{{ roleLabel }}</span>
              </div>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

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
  ArrowDown, Trophy, Plus, Edit,
  Document, DataAnalysis, View, Stamp, Setting
} from '@element-plus/icons-vue'
import NotificationBell from '@/components/NotificationBell.vue'

const route = useRoute()
const router = useRouter()

const userInfo = computed(() => {
  try { return JSON.parse(localStorage.getItem('userInfo') || '{}') }
  catch { return {} }
})

const avatarLetter = computed(() => {
  const name = userInfo.value.realName || userInfo.value.username || 'U'
  return name.charAt(0).toUpperCase()
})

const roleLabel = computed(() => {
  const map = { STUDENT: '学生', TEACHER: '教师', SECRETARY: '科研秘书', LEADER: '学院领导', ADMIN: '系统管理员' }
  return map[userInfo.value.role] || '用户'
})

const isAchievementActive = computed(() =>
  ['/competition/list', '/project/list', '/paper/list', '/software/list'].some(p => route.path.startsWith(p))
  || ['/competition/detail', '/project/detail', '/paper/detail', '/software/detail'].some(p => route.path.startsWith(p))
)

const isSystemActive = computed(() =>
  route.path.startsWith('/admin')
)

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
  justify-content: space-between;
  background: var(--nav-bg);
  border-bottom: 1px solid var(--border-color);
  padding: 0 var(--page-padding);
  flex-shrink: 0;
  position: relative;
  z-index: 100;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 40px;
  height: 100%;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text-primary);
  flex-shrink: 0;
}

.logo-icon {
  width: 26px;
  height: 26px;
  color: var(--primary-color);
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
}

/* 导航项 */
.nav-links {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  height: var(--nav-height);
  padding: 0 14px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-regular);
  text-decoration: none;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: color var(--transition-fast), border-color var(--transition-fast);
  white-space: nowrap;
}

.nav-item:hover {
  color: var(--primary-color);
}

.nav-item.active {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
}

.nav-item .arrow {
  transition: transform var(--transition-fast);
  font-size: 12px;
  margin-left: -2px;
}

/* 右侧 */
.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

/* 用户头像 */
.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: box-shadow var(--transition-fast);
}

.user-avatar:hover {
  box-shadow: 0 0 0 3px var(--primary-light);
}

.avatar-text {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
}

/* 下拉用户信息 */
.dropdown-user-info {
  padding: 12px 16px 10px;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 4px;
}

.dropdown-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.dropdown-role {
  display: block;
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.dropdown-link {
  color: var(--text-regular);
  text-decoration: none;
  display: block;
  width: 100%;
}

/* ========== 主内容区 ========== */
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--page-padding);
  max-width: var(--content-max-width);
  width: 100%;
  margin: 0 auto;
}
</style>
