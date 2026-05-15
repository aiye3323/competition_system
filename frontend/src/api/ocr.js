import request from '@/utils/request'

export function recognizeOcr(file, type) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  return request.post('/ocr/recognize', formData, {
    timeout: 30000
  })
}

/** 通过已上传的文件 ID 进行 OCR 识别 */
export function recognizeOcrById(fileId, type) {
  const formData = new FormData()
  formData.append('fileId', fileId)
  formData.append('type', type)
  return request.post('/ocr/recognize-by-id', formData, {
    timeout: 30000
  })
}
