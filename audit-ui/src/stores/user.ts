import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo as getInfoApi } from '@/api/auth'

export interface UserInfo {
  userId: number
  username: string
  realName: string
  phone?: string
  email?: string
  userType: number
  enterpriseId?: number
  enterpriseName?: string
  auditYear?: number
  passwordChanged: boolean
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const portalPath = computed(() => {
    if (!userInfo.value) return '/login'
    switch (userInfo.value.userType) {
      case 1: return '/admin/dashboard'
      case 2: return '/auditor/dashboard'
      case 3: return '/enterprise/dashboard'
      default: return '/login'
    }
  })
  const needChangePassword = computed(() => userInfo.value?.passwordChanged === false)

  async function login(form: { username: string; password: string; portal: string }) {
    const res = await loginApi(form)
    token.value = res.token
    localStorage.setItem('token', res.token)
    localStorage.setItem('userType', String(res.userType))
    userInfo.value = {
      userId: res.userId,
      username: res.username,
      realName: res.realName,
      userType: res.userType,
      enterpriseId: res.enterpriseId,
      enterpriseName: res.enterpriseName,
      passwordChanged: res.passwordChanged,
    } as UserInfo
    return res
  }

  async function fetchUserInfo() {
    const res = await getInfoApi()
    userInfo.value = {
      userId: res.userId,
      username: res.username,
      realName: res.realName,
      phone: res.phone,
      email: res.email,
      userType: res.userType,
      enterpriseId: res.enterpriseId,
      enterpriseName: res.enterpriseName,
      auditYear: res.auditYear,
      passwordChanged: res.passwordChanged,
    }
    return res
  }

  function logout() {
    logoutApi().catch(() => {})
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userType')
  }

  return { token, userInfo, isLoggedIn, portalPath, needChangePassword, login, fetchUserInfo, logout }
})
