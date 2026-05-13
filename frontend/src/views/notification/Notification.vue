<template>
  <div class="page-container" style="max-width:700px;">
    <h2>消息通知</h2>

    <div v-if="loading" v-loading="loading" style="min-height:200px;" />

    <el-empty v-else-if="list.length === 0" description="暂无通知" />

    <div v-else>
      <div style="text-align:right; margin-bottom:12px;">
        <el-button size="small" @click="markAllRead">全部标记已读</el-button>
      </div>

      <el-card
        v-for="item in list"
        :key="item.id"
        :shadow="item.isRead ? 'never' : 'hover'"
        :style="{ marginBottom: '8px', cursor: 'pointer', background: item.isRead ? 'var(--card-bg)' : 'var(--hover-bg)' }"
        @click="handleClick(item)"
      >
        <div style="display:flex; justify-content:space-between; align-items:flex-start;">
          <div>
            <span :style="{ fontWeight: item.isRead ? 'normal' : 'bold' }">{{ item.title }}</span>
            <p v-if="item.content" style="margin:4px 0 0;" class="text-regular">{{ item.content }}</p>
          </div>
          <div style="text-align:right; white-space:nowrap; margin-left:16px;">
            <el-tag v-if="!item.isRead" size="small" type="danger" style="margin-bottom:4px;">未读</el-tag>
            <div class="text-secondary" style="font-size:12px;">{{ item.createTime }}</div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNotifications, markNotificationRead } from '@/api/audit'

const router = useRouter()
const list = ref([])
const loading = ref(false)

onMounted(async () => {
  await fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getNotifications()
    if (res.code === 200) {
      list.value = res.data
    }
  } finally {
    loading.value = false
  }
}

async function handleClick(item) {
  if (!item.isRead) {
    await markNotificationRead(item.id)
    item.isRead = 1
  }
  if (item.competitionId) {
    router.push(`/competition/detail/${item.competitionId}`)
  }
}

async function markAllRead() {
  for (const item of list.value) {
    if (!item.isRead) {
      await markNotificationRead(item.id)
      item.isRead = 1
    }
  }
  ElMessage.success('已全部标记已读')
}
</script>
