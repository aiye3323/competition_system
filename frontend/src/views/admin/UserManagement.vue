<template>
  <div>
    <div class="page-header"><h2>用户管理</h2></div>

    <el-card class="card-wrapper">
      <el-form :inline="true">
        <el-form-item label="角色">
          <el-select v-model="filters.role" placeholder="全部" clearable style="width:140px;" @change="fetchUsers">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="科研秘书" value="SECRETARY" />
            <el-option label="学院领导" value="LEADER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="filters.keyword" placeholder="搜索姓名/用户名/学号" clearable style="width:200px;" @keyup.enter="fetchUsers" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchUsers">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="users" v-loading="loading" stripe border style="border-radius:12px;">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="100" />
      <el-table-column prop="realName" label="姓名" width="90" />
      <el-table-column prop="studentId" label="学号/工号" width="110" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-select :model-value="row.role" @change="(v) => handleRoleChange(row, v)" size="small" style="width:110px;">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="科研秘书" value="SECRETARY" />
            <el-option label="学院领导" value="LEADER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" width="170" show-overflow-tooltip />
      <el-table-column prop="college" label="学院" width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button
            :type="row.status === 1 ? 'danger' : 'success'"
            size="small"
            @click="handleToggle(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchUsers"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const users = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(20)
const filters = reactive({ role: '', keyword: '' })

async function fetchUsers() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.role) params.role = filters.role
    if (filters.keyword) params.keyword = filters.keyword
    const res = await request.get('/admin/users', { params })
    if (res.code === 200) {
      users.value = res.data.content
      total.value = res.data.totalElements
    }
  } finally {
    loading.value = false
  }
}

async function handleToggle(row) {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}用户 "${row.username}"？`, '提示')
    const res = await request.put(`/admin/users/${row.id}/status`)
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      fetchUsers()
    }
  } catch { /* cancel */ }
}

async function handleRoleChange(row, newRole) {
  try {
    await ElMessageBox.confirm(`确定将 "${row.username}" 的角色改为 ${roleLabel(newRole)}？`, '提示')
    const res = await request.put(`/admin/users/${row.id}/role`, { role: newRole })
    if (res.code === 200) {
      row.role = newRole
      ElMessage.success('角色修改成功')
    }
  } catch { /* cancel */ }
}

function roleLabel(r) {
  return { STUDENT: '学生', TEACHER: '教师', SECRETARY: '科研秘书', LEADER: '学院领导', ADMIN: '管理员' }[r] || r
}

onMounted(() => fetchUsers())
</script>
