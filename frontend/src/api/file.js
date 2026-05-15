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

/** 打包下载单个成果的所有文件 */
export function downloadAllFiles(relatedType, relatedId) {
  return `${request.defaults.baseURL}/files/download-all/${relatedType}/${relatedId}`
}

/** 打包下载选中的文件（返回 blob） */
export function downloadSelectedFiles(fileIds) {
  return request.post('/files/download-selected', { fileIds }, {
    responseType: 'blob'
  })
}
