<template>
  <div class="page-container" style="max-width:600px;">
    <h2>个人信息</h2>
    <el-card class="card-wrapper" v-loading="loading">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名">
          <el-input :model-value="form.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag>{{ roleLabel(form.role) }}</el-tag>
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="学号/工号">
          <el-input v-model="form.studentId" placeholder="选填" />
        </el-form-item>
        <el-form-item label="学院">
          <el-select v-model="form.college" placeholder="请选择" style="width:100%" clearable>
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
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  username: '',
  realName: '',
  email: '',
  studentId: '',
  college: '',
  role: ''
})

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await request.get('/auth/profile')
    if (res.code === 200) {
      Object.assign(form, res.data)
    }
  } finally {
    loading.value = false
  }
})

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const res = await request.put('/auth/profile', {
      realName: form.realName,
      email: form.email,
      studentId: form.studentId,
      college: form.college
    })
    if (res.code === 200) {
      // Update localStorage userInfo
      const ui = JSON.parse(localStorage.getItem('userInfo') || '{}')
      ui.realName = res.data.realName
      localStorage.setItem('userInfo', JSON.stringify(ui))
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

function roleLabel(role) {
  const m = { STUDENT: '学生', TEACHER: '教师', SECRETARY: '科研秘书', LEADER: '学院领导', ADMIN: '系统管理员' }
  return m[role] || role
}
</script>
