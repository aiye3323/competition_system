<template>
  <div>
    <div class="page-header"><h2>操作日志</h2></div>

    <el-card class="card-wrapper">
      <el-form :inline="true" :model="filters">
        <el-form-item label="操作类型">
          <el-select v-model="filters.operationType" placeholder="全部" clearable style="width:160px">
            <el-option label="登录" value="LOGIN" />
            <el-option label="注册" value="REGISTER" />
            <el-option label="提交" value="SUBMIT" />
            <el-option label="修改" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="审核" value="AUDIT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="filters.operationType='';fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="operateTime" label="操作时间" width="170">
        <template #default="{ row }">{{ formatTime(row.operateTime) }}</template>
      </el-table-column>
      <el-table-column prop="username" label="操作人" width="120" />
      <el-table-column prop="operationType" label="操作类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.operationType)" size="small">{{ row.operationType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="操作描述" min-width="180" />
      <el-table-column prop="targetType" label="对象类型" width="120" />
      <el-table-column prop="targetId" label="对象ID" width="80" />
      <el-table-column prop="ip" label="IP地址" width="140" />
    </el-table>

    <div style="display:flex; justify-content:flex-end; margin-top:20px;">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const filters = reactive({ operationType: '' })

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.operationType) params.operationType = filters.operationType
    const res = await request.get('/logs', { params })
    if (res.code === 200) {
      list.value = res.data.content
      total.value = res.data.totalElements
    }
  } finally {
    loading.value = false
  }
}

function typeTag(type) {
  const m = { LOGIN: 'success', REGISTER: '', SUBMIT: 'primary', UPDATE: 'warning', DELETE: 'danger', AUDIT: 'info' }
  return m[type] || ''
}

function formatTime(time) {
  if (!time) return ''
  return time.substring(0, 19).replace('T', ' ')
}
</script>
