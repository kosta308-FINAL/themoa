import axios from 'axios'

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,
})

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/*
 * Access Token(30분) 만료로 401이 오면 Refresh rotation(/api/auth/refresh) 후
 * 원 요청을 1회 재시도한다. 동시에 여러 요청이 401을 받아도 refresh는 한 번만 나간다.
 * auth 경로 자신의 401(로그인 실패 등)은 재시도 대상이 아니다.
 */
let refreshPromise = null

const isAuthPath = (url = '') => url.includes('/api/auth/')

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    if (error.response?.status !== 401 || !original || original._retry || isAuthPath(original.url)) {
      return Promise.reject(error)
    }
    original._retry = true
    try {
      refreshPromise ??= axiosInstance.post('/api/auth/refresh').finally(() => {
        refreshPromise = null
      })
      const res = await refreshPromise
      const accessToken = res.data?.data?.accessToken
      if (!accessToken) {
        return Promise.reject(error)
      }
      localStorage.setItem('accessToken', accessToken)
      return axiosInstance(original)
    } catch {
      localStorage.removeItem('accessToken')
      return Promise.reject(error)
    }
  }
)

export default axiosInstance
