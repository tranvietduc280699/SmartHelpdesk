/**
 * Nơi duy nhất đọc/ghi phiên đăng nhập (access token, refresh token, hồ sơ).
 *
 * Tách riêng khỏi http.ts và store để không tạo phụ thuộc vòng
 * (store cần http để gọi API, http cần token mà store đang giữ).
 *
 * "Ghi nhớ đăng nhập" quyết định phiên nằm ở đâu:
 *   - có nhớ  → localStorage, còn sau khi đóng trình duyệt
 *   - không   → sessionStorage, mất khi đóng tab
 *
 * Vì sao lưu cả hồ sơ người dùng chứ không chỉ token: backend chưa có
 * endpoint kiểu GET /auth/me, mà JWT chỉ mang `sub` (email) và claim `id` —
 * không có name/role. Không lưu lại thì sau khi F5 router guard không biết
 * vai trò để định tuyến. Hồ sơ trong storage chỉ dùng cho giao diện; mọi
 * quyền thật vẫn do backend kiểm tra trên từng request.
 *
 * Lưu ý bảo mật: localStorage/sessionStorage đều đọc được bằng JavaScript
 * nên không chống được XSS. Phương án tốt hơn là cookie HttpOnly + SameSite;
 * lúc đó chỉ cần sửa file này.
 */
import type { AuthUser } from '@/types/user'

const ACCESS_KEY = 'kits.auth.accessToken'
const REFRESH_KEY = 'kits.auth.refreshToken'
const USER_KEY = 'kits.auth.user'
const KEYS = [ACCESS_KEY, REFRESH_KEY, USER_KEY]

export interface StoredSession {
  accessToken: string
  refreshToken: string
  user: AuthUser
}

/** localStorage có thể ném lỗi (Safari riêng tư, cookie bị chặn). */
function safe<T>(action: () => T, fallback: T): T {
  try {
    return action()
  } catch {
    return fallback
  }
}

function read(key: string): string | null {
  return safe(
    () => localStorage.getItem(key) ?? sessionStorage.getItem(key),
    null,
  )
}

/**
 * Kho đang giữ phiên hiện tại. Dùng khi làm mới token: token mới phải ghi
 * đúng chỗ token cũ, nếu không người chọn "không ghi nhớ" sẽ bị đẩy phiên
 * sang localStorage và phiên sống dai hơn họ muốn.
 */
function activeStore(): Storage | null {
  return safe(
    () =>
      localStorage.getItem(ACCESS_KEY) !== null
        ? localStorage
        : sessionStorage.getItem(ACCESS_KEY) !== null
          ? sessionStorage
          : null,
    null,
  )
}

export function getAccessToken(): string | null {
  return read(ACCESS_KEY)
}

export function getRefreshToken(): string | null {
  return read(REFRESH_KEY)
}

export function getStoredUser(): AuthUser | null {
  const raw = read(USER_KEY)
  if (!raw) return null
  return safe<AuthUser | null>(() => {
    const user = JSON.parse(raw) as AuthUser
    return user &&
      typeof user.id === 'string' &&
      typeof user.email === 'string' &&
      typeof user.name === 'string' &&
      ['admin', 'dev', 'client'].includes(user.role)
      ? user
      : null
  }, null)
}

export function saveSession(session: StoredSession, remember = true): void {
  // Dọn cả hai trước để không sót phiên cũ ở kho còn lại.
  clearSession()
  safe(() => {
    const store = remember ? localStorage : sessionStorage
    store.setItem(ACCESS_KEY, session.accessToken)
    store.setItem(REFRESH_KEY, session.refreshToken)
    store.setItem(USER_KEY, JSON.stringify(session.user))
  }, undefined)
}

/** Cập nhật cặp token sau khi gọi /auth/refresh, giữ nguyên kho đang dùng. */
export function updateTokens(accessToken: string, refreshToken: string): void {
  const store = activeStore()
  if (!store) return
  safe(() => {
    store.setItem(ACCESS_KEY, accessToken)
    store.setItem(REFRESH_KEY, refreshToken)
  }, undefined)
}

export function clearSession(): void {
  safe(() => {
    for (const key of KEYS) {
      localStorage.removeItem(key)
      sessionStorage.removeItem(key)
    }
  }, undefined)
}
