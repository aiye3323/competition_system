import request from '@/utils/request'

export function secretaryReview(id, data) {
  return request.post(`/competitions/${id}/secretary-review`, data)
}

export function leaderReview(id, data) {
  return request.post(`/competitions/${id}/leader-review`, data)
}

export function getAuditLogs(id) {
  return request.get(`/competitions/${id}/audit-logs`)
}
