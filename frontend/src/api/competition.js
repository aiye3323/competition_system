import request from '@/utils/request'

export function submitCompetition(data) {
  return request.post('/competitions', data)
}

export function getCompetitionList(params) {
  return request.get('/competitions', { params })
}

export function getCompetitionDetail(id) {
  return request.get(`/competitions/${id}`)
}

export function updateCompetition(id, data) {
  return request.put(`/competitions/${id}`, data)
}

export function deleteCompetition(id) {
  return request.delete(`/competitions/${id}`)
}

export function exportCompetition(params) {
  return request.get('/competitions/export', { params, responseType: 'blob' })
}
