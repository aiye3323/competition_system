<template>
  <div class="register-page">
    <div class="register-bg">
      <div class="bg-shape bg-shape-1"></div>
      <div class="bg-shape bg-shape-2"></div>
    </div>
    <div class="register-container">
      <div class="register-card">
        <div class="register-header">
          <svg class="register-logo" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 10v6M2 10l10-5 10 5-10 5z"/>
            <path d="M6 12v5c0 1.1 2.7 2 6 2s6-.9 6-2v-5"/>
          </svg>
          <h1>创建账号</h1>
          <p>注册后即可提交和管理科研成果</p>
        </div>
        <el-form
          ref="formRef"
          :model="registerForm"
          :rules="rules"
          label-width="0"
          size="large"
        >
          <el-form-item prop="username">
            <el-input v-model="registerForm.username" placeholder="用户名（3-50个字符）" />
          </el-form-item>
          <el-form-item prop="realName">
            <el-input v-model="registerForm.realName" placeholder="真实姓名" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item prop="password">
                <el-input v-model="registerForm.password" type="password" placeholder="密码（至少6位）" show-password />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item prop="confirmPassword">
                <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" show-password />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item prop="role">
            <el-select v-model="registerForm.role" placeholder="选择身份" style="width:100%">
              <el-option label="学生" value="STUDENT" />
              <el-option label="教师" value="TEACHER" />
            </el-select>
          </el-form-item>
          <el-form-item prop="email">
            <el-input v-model="registerForm.email" placeholder="邮箱地址" />
          </el-form-item>
          <el-form-item prop="studentId">
            <el-input v-model="registerForm.studentId" placeholder="学号/工号（选填）" />
          </el-form-item>
          <el-form-item prop="college">
            <el-select v-model="registerForm.college" placeholder="所属学院" style="width:100%" clearable>
              <el-option label="马克思主义学院" value="马克思主义学院" />
              <el-option label="数字经济与管理学院" value="数字经济与管理学院" />
              <el-option label="法学与公共管理学院" value="法学与公共管理学院" />
              <el-option label="教育科学学院（预科教学部）" value="教育科学学院（预科教学部）" />
              <el-option label="体育与健康学院" value="体育与健康学院" />
              <el-option label="文学与新闻传播学院" value="文学与新闻传播学院" />
              <el-option label="外国语学院" value="外国语学院" />
              <el-option label="数学与计算科学学院" value="数学与计算科学学院" />
              <el-option label="物电与智能制造学院" value="物电与智能制造学院" />
              <el-option label="化学与材料工程学院" value="化学与材料工程学院" />
              <el-option label="生物与食品工程学院" value="生物与食品工程学院" />
              <el-option label="计算机与人工智能学院（软件学院）" value="计算机与人工智能学院（软件学院）" />
              <el-option label="音乐舞蹈学院" value="音乐舞蹈学院" />
              <el-option label="美术与设计艺术学院" value="美术与设计艺术学院" />
              <el-option label="国际学院" value="国际学院" />
              <el-option label="继续教育学院" value="继续教育学院" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" class="register-btn" @click="handleRegister">
              注册
            </el-button>
          </el-form-item>
        </el-form>
        <div class="register-footer">
          已有账号？<router-link to="/login">立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  role: '',
  email: '',
  studentId: '',
  college: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const { confirmPassword, ...submitData } = registerForm
    const res = await request.post('/auth/register', submitData)
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, var(--login-bg-start) 0%, var(--login-bg-end) 100%);
  position: relative;
  overflow: hidden;
}

.register-bg {
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

.register-container {
  position: relative;
  z-index: 1;
  padding: 60px 0;
}

.register-card {
  width: 500px;
  background: #fff;
  border-radius: 12px;
  padding: 44px 44px 36px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.18);
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-logo {
  width: 40px;
  height: 40px;
  color: var(--primary-color);
  margin-bottom: 14px;
}

.register-header h1 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.01em;
  margin: 0 0 6px;
}

.register-header p {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.register-footer {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
}

.register-footer a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.register-footer a:hover {
  color: var(--primary-dark);
}
</style>
