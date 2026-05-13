import request from '@/utils/request'

export function submitProject(data) {
  return request.post('/projects', data)
}

export function getProjectList(params) {
  return request.get('/projects', { params })
}

export function getProjectDetail(id) {
  return request.get(`/projects/${id}`)
}

export function updateProject(id, data) {
  return request.put(`/projects/${id}`, data)
}

export function deleteProject(id) {
  return request.delete(`/projects/${id}`)
}

export function getProjectPendingList(params) {
  return request.get('/projects/pending', { params })
}

export function exportProject(params) {
  return request.get('/projects/export', { params, responseType: 'blob' })
}
