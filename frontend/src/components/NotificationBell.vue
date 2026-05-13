<template>
  <el-popover placement="bottom" :width="320" trigger="click" @hide="handlePopoverHide">
    <template #reference>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="bell-badge">
        <el-icon :size="22" style="cursor:pointer; color:var(--text-primary);">
          <Bell />
        </el-icon>
      </el-badge>
    </template>

    <div style="max-height:360px; overflow-y:auto;">
      <div v-if="recentList.length === 0" class="text-secondary" style="text-align:center; padding:20px;">
        暂无通知
      </div>
      <div
        v-for="item in recentList"
        :key="item.id"
        style="padding:8px 0; cursor:pointer; border-bottom:1px solid var(--border-color);"
        @click="handleItemClick(item)"
      >
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <span :style="{ fontWeight: item.isRead ? 'normal' : 'bold', fontSize: '13px' }">
            {{ item.title }}
          </span>
          <el-tag v-if="!item.isRead" size="small" type="danger" style="flex-shrink:0; margin-left:8px;">新</el-tag>
        </div>
        <div class="text-secondary" style="font-size:12px; margin-top:2px;">{{ formatTime(item.createTime) }}</div>
      </div>
      <div style="text-align:center; margin-top:8px;">
        <el-button text size="small" @click="goToAll">查看全部</el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { getNotifications, getUnreadCount, markNotificationRead } from '@/api/audit'

const router = useRouter()
const unreadCount = ref(0)
const recentList = ref([])
let pollTimer = null

onMounted(() => {
  refresh()
  pollTimer = setInterval(refresh, 60000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

async function refresh() {
  try {
    const countRes = await getUnreadCount()
    if (countRes.code === 200) {
      unreadCount.value = countRes.data
    }
    const notifRes = await getNotifications()
    if (notifRes.code === 200) {
      recentList.value = notifRes.data.slice(0, 5)
    }
  } catch {
    // ignore polling errors
  }
}

async function handleItemClick(item) {
  if (!item.isRead) {
    await markNotificationRead(item.id)
    item.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  if (item.competitionId) {
    router.push(`/competition/detail/${item.competitionId}`)
  }
}

function goToAll() {
  router.push('/notifications')
}

function formatTime(time) {
  if (!time) return ''
  return time.substring(0, 16).replace('T', ' ')
}

function handlePopoverHide() {
  refresh()
}
</script>

<style scoped>
.bell-badge {
  margin-right: 16px;
}
</style>
