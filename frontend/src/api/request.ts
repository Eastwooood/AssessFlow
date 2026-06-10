import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code !== 200) {
      alert(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  (err) => {
    alert(err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default request
