import request from '@/utils/request'

export function submitPaper(data) {
  return request.post('/papers', data)
}

export function getPaperList(params) {
  return request.get('/papers', { params })
}

export function getPaperDetail(id) {
  return request.get(`/papers/${id}`)
}

export function updatePaper(id, data) {
  return request.put(`/papers/${id}`, data)
}

export function deletePaper(id) {
  return request.delete(`/papers/${id}`)
}

export function getPaperPendingList(params) {
  return request.get('/papers/pending', { params })
}

export function exportPaper(params) {
  return request.get('/papers/export', { params, responseType: 'blob' })
}
