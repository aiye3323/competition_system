import request from '@/utils/request'

export function submitSoftware(data) {
  return request.post('/software', data)
}

export function getSoftwareList(params) {
  return request.get('/software', { params })
}

export function getSoftwareDetail(id) {
  return request.get(`/software/${id}`)
}

export function updateSoftware(id, data) {
  return request.put(`/software/${id}`, data)
}

export function deleteSoftware(id) {
  return request.delete(`/software/${id}`)
}

export function getSoftwarePendingList(params) {
  return request.get('/software/pending', { params })
}

export function exportSoftware(params) {
  return request.get('/software/export', { params, responseType: 'blob' })
}
