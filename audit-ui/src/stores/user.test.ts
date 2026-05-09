import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useUserStore } from './user'

vi.mock('@/api/auth', () => ({
  login: vi.fn(async () => ({
    token: 'token-enterprise',
    userId: 7,
    username: 'enterprise',
    realName: 'Enterprise User',
    userType: 3,
    enterpriseId: 88,
    enterpriseName: '测试企业',
    passwordChanged: false,
  })),
  logout: vi.fn(async () => undefined),
  getUserInfo: vi.fn(async () => ({
    userId: 7,
    username: 'enterprise',
    realName: 'Enterprise User',
    userType: 3,
    enterpriseId: 88,
    enterpriseName: '测试企业',
    auditYear: 2026,
    passwordChanged: true,
  })),
}))

describe('user store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('persists login token and resolves enterprise portal path', async () => {
    const store = useUserStore()

    await store.login({ username: 'enterprise', password: 'password', portal: 'enterprise' })

    expect(store.isLoggedIn).toBe(true)
    expect(store.portalPath).toBe('/enterprise/dashboard')
    expect(store.needChangePassword).toBe(true)
    expect(localStorage.getItem('token')).toBe('token-enterprise')
    expect(localStorage.getItem('userType')).toBe('3')
  })

  it('clears local auth state on logout', async () => {
    const store = useUserStore()
    await store.login({ username: 'enterprise', password: 'password', portal: 'enterprise' })

    store.logout()

    expect(store.isLoggedIn).toBe(false)
    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('userType')).toBeNull()
  })
})
