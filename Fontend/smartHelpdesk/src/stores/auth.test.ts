import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import { authService } from '@/services/authService'
import type { AuthUser, Member } from '@/types/user'

vi.mock('@/services/authService', () => ({
  authService: {
    login: vi.fn(),
    register: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
  },
}))

const ACCESS_KEY = 'kits.auth.accessToken'
const REFRESH_KEY = 'kits.auth.refreshToken'
const USER_KEY = 'kits.auth.user'

const CLIENT: AuthUser = {
  id: '11111111-1111-4111-8111-111111111111',
  name: 'Trần Thị Khách',
  email: 'client@kits.vn',
  role: 'client',
  companyId: 'KR_CLIENT_Ss',
}

const ADMIN: AuthUser = {
  ...CLIENT,
  id: '33333333-3333-4333-8333-333333333333',
  name: 'Nguyễn Quản Trị',
  email: 'admin@kits.vn',
  role: 'admin',
}

const mocked = vi.mocked(authService)

function session(user: AuthUser, suffix = '1') {
  return {
    accessToken: `access-${suffix}`,
    refreshToken: `refresh-${suffix}`,
    user,
  }
}

/** Đặt sẵn một phiên trong storage như thể người dùng vừa F5 trang. */
function seedStorage(store: Storage, user: AuthUser): void {
  store.setItem(ACCESS_KEY, 'access-cu')
  store.setItem(REFRESH_KEY, 'refresh-cu')
  store.setItem(USER_KEY, JSON.stringify(user))
}

describe('store auth', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    vi.clearAllMocks()
    mocked.logout.mockResolvedValue(undefined)
    setActivePinia(createPinia())
  })

  it('bắt đầu ở trạng thái chưa đăng nhập', () => {
    const auth = useAuthStore()
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.role).toBeNull()
  })

  it('login() lưu cả hai token và hồ sơ vào localStorage khi chọn ghi nhớ', async () => {
    mocked.login.mockResolvedValue(session(CLIENT))
    const auth = useAuthStore()

    await auth.login({ email: CLIENT.email, password: '12345678' }, true)

    expect(auth.isAuthenticated).toBe(true)
    expect(auth.role).toBe('client')
    expect(localStorage.getItem(ACCESS_KEY)).toBe('access-1')
    expect(localStorage.getItem(REFRESH_KEY)).toBe('refresh-1')
    expect(JSON.parse(localStorage.getItem(USER_KEY)!)).toEqual(CLIENT)
    expect(sessionStorage.getItem(ACCESS_KEY)).toBeNull()
  })

  it('login() dùng sessionStorage khi không ghi nhớ', async () => {
    mocked.login.mockResolvedValue(session(CLIENT, '2'))
    const auth = useAuthStore()

    await auth.login({ email: CLIENT.email, password: '12345678' }, false)

    expect(sessionStorage.getItem(ACCESS_KEY)).toBe('access-2')
    expect(sessionStorage.getItem(REFRESH_KEY)).toBe('refresh-2')
    expect(localStorage.getItem(ACCESS_KEY)).toBeNull()
  })

  it('logout() báo backend thu hồi token rồi dọn sạch cả hai kho', async () => {
    mocked.login.mockResolvedValue(session(CLIENT, '3'))
    const auth = useAuthStore()
    await auth.login({ email: CLIENT.email, password: '12345678' })

    await auth.logout()

    expect(mocked.logout).toHaveBeenCalledOnce()
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.user).toBeNull()
    expect(localStorage.getItem(ACCESS_KEY)).toBeNull()
    expect(localStorage.getItem(USER_KEY)).toBeNull()
    expect(sessionStorage.getItem(ACCESS_KEY)).toBeNull()
  })

  it('logout() vẫn đăng xuất tại máy khi gọi backend hỏng', async () => {
    mocked.login.mockResolvedValue(session(CLIENT, '4'))
    mocked.logout.mockRejectedValue(new Error('mất mạng'))
    const auth = useAuthStore()
    await auth.login({ email: CLIENT.email, password: '12345678' })

    await auth.logout()

    expect(auth.isAuthenticated).toBe(false)
    expect(localStorage.getItem(ACCESS_KEY)).toBeNull()
  })

  it('restore() đọc lại phiên từ storage, không gọi API', () => {
    seedStorage(localStorage, ADMIN)
    const auth = useAuthStore()

    // Store đọc storage ngay lúc khởi tạo — không cần chờ vòng gọi API nào.
    expect(auth.isAuthenticated).toBe(true)
    expect(auth.role).toBe('admin')

    auth.restore()

    expect(auth.user).toEqual(ADMIN)
    expect(mocked.login).not.toHaveBeenCalled()
  })

  it('restore() đăng xuất khi phiên trong storage bị thiếu hồ sơ', () => {
    localStorage.setItem(ACCESS_KEY, 'access-mo-coi')
    const auth = useAuthStore()

    expect(auth.hasToken).toBe(true)
    expect(auth.isAuthenticated).toBe(false)

    auth.restore()

    expect(auth.hasToken).toBe(false)
    expect(localStorage.getItem(ACCESS_KEY)).toBeNull()
  })

  it('register() không tự đăng nhập — backend không trả token', async () => {
    const created: Member = {
      ...CLIENT,
      phone: null,
      status: 'active',
      createdAt: '2026-08-06T09:00:00Z',
    }
    mocked.register.mockResolvedValue(created)
    const auth = useAuthStore()

    const member = await auth.register({
      name: CLIENT.name,
      email: CLIENT.email,
      phone: null,
      password: '12345678',
      role: 'client',
      companyId: CLIENT.companyId,
    })

    expect(member).toEqual(created)
    expect(auth.isAuthenticated).toBe(false)
    expect(localStorage.getItem(ACCESS_KEY)).toBeNull()
  })

  it('hasRole() phân biệt đúng vai trò', async () => {
    mocked.login.mockResolvedValue(session(CLIENT, '5'))
    const auth = useAuthStore()
    await auth.login({ email: CLIENT.email, password: '12345678' })

    expect(auth.hasRole('client')).toBe(true)
    expect(auth.hasRole('client', 'admin')).toBe(true)
    expect(auth.hasRole('admin')).toBe(false)
  })
})
