import request from '@/utils/request'

export function secretaryApprove(id, data) {
  return request.post(`/audit/secretary/approve/${id}`, data)
}

export function secretaryReject(id, data) {
  return request.post(`/audit/secretary/reject/${id}`, data)
}

export function leaderApprove(id, data) {
  return request.post(`/audit/leader/approve/${id}`, data)
}

export function leaderReject(id, data) {
  return request.post(`/audit/leader/reject/${id}`, data)
}

export function getAuditLogs(id) {
  return request.get(`/audit/competition/${id}`)
}

export function getPendingList(params) {
  return request.get('/competitions/pending', { params })
}

export function getNotifications() {
  return request.get('/notifications')
}

export function markNotificationRead(id) {
  return request.put(`/notifications/${id}/read`)
}

export function getUnreadCount() {
  return request.get('/notifications/unread-count')
}

// ==================== Project audit ====================

export function secretaryApproveProject(id, data) {
  return request.post(`/audit/secretary/project/${id}/approve`, data)
}

export function secretaryRejectProject(id, data) {
  return request.post(`/audit/secretary/project/${id}/reject`, data)
}

export function leaderApproveProject(id, data) {
  return request.post(`/audit/leader/project/${id}/approve`, data)
}

export function leaderRejectProject(id, data) {
  return request.post(`/audit/leader/project/${id}/reject`, data)
}

export function getProjectAuditLogs(id) {
  return request.get(`/audit/project/${id}`)
}

// ==================== Paper audit ====================

export function secretaryApprovePaper(id, data) {
  return request.post(`/audit/secretary/paper/${id}/approve`, data)
}

export function secretaryRejectPaper(id, data) {
  return request.post(`/audit/secretary/paper/${id}/reject`, data)
}

export function leaderApprovePaper(id, data) {
  return request.post(`/audit/leader/paper/${id}/approve`, data)
}

export function leaderRejectPaper(id, data) {
  return request.post(`/audit/leader/paper/${id}/reject`, data)
}

export function getPaperAuditLogs(id) {
  return request.get(`/audit/paper/${id}`)
}

// ==================== Software audit ====================

export function secretaryApproveSoftware(id, data) {
  return request.post(`/audit/secretary/software/${id}/approve`, data)
}

export function secretaryRejectSoftware(id, data) {
  return request.post(`/audit/secretary/software/${id}/reject`, data)
}

export function leaderApproveSoftware(id, data) {
  return request.post(`/audit/leader/software/${id}/approve`, data)
}

export function leaderRejectSoftware(id, data) {
  return request.post(`/audit/leader/software/${id}/reject`, data)
}

export function getSoftwareAuditLogs(id) {
  return request.get(`/audit/software/${id}`)
}

// ==================== Unified audit API ====================

export function getUnifiedPendingList(params) {
  return request.get('/audit/pending-list', { params })
}

export function getAuditDetail(type, id) {
  return request.get(`/audit/detail/${type}/${id}`)
}

export function secretaryApproveItem(type, id, data) {
  if (type === 'COMPETITION') {
    return request.post(`/audit/secretary/approve/${id}`, data)
  }
  return request.post(`/audit/secretary/${type.toLowerCase()}/${id}/approve`, data)
}

export function secretaryRejectItem(type, id, data) {
  if (type === 'COMPETITION') {
    return request.post(`/audit/secretary/reject/${id}`, data)
  }
  return request.post(`/audit/secretary/${type.toLowerCase()}/${id}/reject`, data)
}

export function leaderApproveItem(type, id, data) {
  if (type === 'COMPETITION') {
    return request.post(`/audit/leader/approve/${id}`, data)
  }
  return request.post(`/audit/leader/${type.toLowerCase()}/${id}/approve`, data)
}

export function leaderRejectItem(type, id, data) {
  if (type === 'COMPETITION') {
    return request.post(`/audit/leader/reject/${id}`, data)
  }
  return request.post(`/audit/leader/${type.toLowerCase()}/${id}/reject`, data)
}

export function getAuditLogsForItem(type, id) {
  return request.get(`/audit/${type.toLowerCase()}/${id}`)
}
