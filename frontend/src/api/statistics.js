import request from '@/utils/request'

export function getDashboardStats() {
  return request.get('/statistics/dashboard')
}

export function getPublicAchievementList(params) {
  return request.get('/achievements/public', { params })
}

export function exportAchievements(params) {
  return request.get('/achievements/export', {
    params,
    responseType: 'blob'
  })
}
