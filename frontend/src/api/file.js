import request from '@/utils/request'

export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload', formData)
}

export function uploadFiles(files) {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  return request.post('/files/upload/batch', formData)
}

export function getFileUrl(id) {
  return `${request.defaults.baseURL}/files/${id}`
}

export function deleteFile(id) {
  return request.delete(`/files/${id}`)
}
