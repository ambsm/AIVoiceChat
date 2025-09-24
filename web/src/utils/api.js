import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080',
  timeout: 60000, // 增加超时时间以支持流式响应
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    console.log('请求发送:', config)
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    console.log('响应接收:', response)
    return response.data
  },
  error => {
    console.error('响应错误:', error)
    // 如果是网络错误或超时，提供更友好的错误信息
    if (error.code === 'ECONNABORTED') {
      error.message = '请求超时，请检查网络连接'
    } else if (error.response) {
      error.message = `服务器错误: ${error.response.status}`
    } else if (error.request) {
      error.message = '网络连接失败，请检查网络设置'
    }
    return Promise.reject(error)
  }
)

export default api