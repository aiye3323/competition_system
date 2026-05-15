<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-shape bg-shape-1"></div>
      <div class="bg-shape bg-shape-2"></div>
    </div>
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <svg class="login-logo" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 10v6M2 10l10-5 10 5-10 5z"/>
            <path d="M6 12v5c0 1.1 2.7 2 6 2s6-.9 6-2v-5"/>
          </svg>
          <h1>科研竞赛管理系统</h1>
          <p>高校科研成果全生命周期管理平台</p>
        </div>
        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          label-width="0"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="用户名"
              :prefix-icon="UserIcon"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              :prefix-icon="LockIcon"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>
        <div class="login-footer">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const UserIcon = shallowRef(User)
const LockIcon = shallowRef(Lock)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await request.post('/auth/login', loginForm)
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, var(--login-bg-start) 0%, var(--login-bg-end) 100%);
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.06;
  background: #fff;
}

.bg-shape-1 {
  width: 640px;
  height: 640px;
  top: -200px;
  right: -160px;
}

.bg-shape-2 {
  width: 400px;
  height: 400px;
  bottom: -120px;
  left: -80px;
}

.login-container {
  position: relative;
  z-index: 1;
}

.login-card {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 48px 44px 40px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.18);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-logo {
  width: 44px;
  height: 44px;
  color: var(--primary-color);
  margin-bottom: 16px;
}

.login-header h1 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.01em;
  margin: 0 0 8px;
}

.login-header p {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.login-footer {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
}

.login-footer a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.login-footer a:hover {
  color: var(--primary-dark);
}
</style>
