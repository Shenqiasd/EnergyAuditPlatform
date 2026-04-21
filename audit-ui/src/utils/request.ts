import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
} from 'axios'
import { ElMessage } from 'element-plus'

// The response interceptor below unwraps `response.data.data` (or
// `response.data`) before returning. Axios's built-in generics can't express
// that, so we expose a narrower typed view of the instance where each HTTP
// method resolves to the unwrapped payload `T` instead of `AxiosResponse<T>`.
//
// Existing callers that relied on the loose `AxiosResponse<any>` return type
// keep working because `T` defaults to `any`.
export interface RequestInstance extends Omit<
  AxiosInstance,
  'get' | 'post' | 'put' | 'delete' | 'patch' | 'head' | 'options' | 'request'
> {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  head<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  options<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  request<T = unknown>(config: AxiosRequestConfig): Promise<T>
}

const instance = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res && typeof res === 'object' && 'code' in res && res.code !== undefined && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // Unwrap to the inner `data` field when the backend envelope is present,
    // otherwise fall through to the raw payload (e.g. Blob downloads).
    if (res && typeof res === 'object' && 'data' in res && res.data !== undefined) {
      return res.data
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

const request = instance as unknown as RequestInstance

export default request
