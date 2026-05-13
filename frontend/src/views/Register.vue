<template>
  <div class="register-container">
    <el-card class="register-card" shadow="always">
      <h2 class="register-title">用户注册</h2>
      <el-form
        ref="formRef"
        :model="registerForm"
        :rules="rules"
        label-width="80px"
        size="large"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="3-50个字符" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="至少6位"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="再次输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="身份" prop="role">
          <el-radio-group v-model="registerForm.role">
            <el-radio value="STUDENT">学生</el-radio>
            <el-radio value="TEACHER">教师</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="学号" prop="studentId">
          <el-input v-model="registerForm.studentId" placeholder="选填" />
        </el-form-item>
        <el-form-item label="学院" prop="college">
          <el-select v-model="registerForm.college" placeholder="请选择" style="width:100%" clearable>
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
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="form-footer">
        已有账号？
        <router-link to="/login">立即登录</router-link>
      </div>
    </el-card>
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
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
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
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, var(--login-gradient-start) 0%, var(--login-gradient-end) 100%);
  position: relative;
}
.register-card {
  width: 520px;
}
.register-title {
  text-align: center;
  margin-bottom: 30px;
  color: var(--text-primary);
}
.form-footer {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
}
.form-footer a {
  color: var(--primary-color);
  text-decoration: none;
}
</style>
