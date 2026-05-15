import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => response.data,
  async error => {
    if (error.response) {
      const { status, data } = error.response
      // blob 响应需解析为 JSON 获取错误消息
      let message = ''
      if (data instanceof Blob && data.type === 'application/json') {
        try {
          const text = await data.text()
          const json = JSON.parse(text)
          message = json.message || ''
        } catch {}
      }
      if (!message && data?.message) message = data.message
      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else {
        ElMessage.error(message || data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查连接')
    }
    return Promise.reject(error)
  }
)

export default request
