import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authService } from '@/services/authService'
import type { LoginPayload, RegisterPayload } from '@/services/authService'
import {
  clearSession,
  getAccessToken,
  getStoredUser,
  saveSession,
} from '@/services/tokenStorage'
import type { AuthUser, Member, UserRole } from '@/types/user'

export const useAuthStore = defineStore('auth', () => {
  /** Người đang đăng nhập, đọc lại từ storage khi app khởi động. */
  const user = ref<AuthUser | null>(getStoredUser())
  const accessToken = ref<string | null>(getAccessToken())

  /** Có token trong máy — chưa chắc còn hiệu lực. */
  const hasToken = computed(() => accessToken.value !== null)
  /** Đã xác thực đầy đủ: có token và có hồ sơ. */
  const isAuthenticated = computed(
    () => accessToken.value !== null && user.value !== null,
  )
  const role = computed<UserRole | null>(() => user.value?.role ?? null)

  function hasRole(...roles: UserRole[]): boolean {
    return role.value !== null && roles.includes(role.value)
  }

  function clearLocal(): void {
    clearSession()
    accessToken.value = null
    user.value = null
  }

  /** `remember = false` giữ phiên trong sessionStorage — mất khi đóng tab. */
  async function login(
    payload: LoginPayload,
    remember = true,
  ): Promise<AuthUser> {
    const session = await authService.login(payload)
    saveSession(
      {
        accessToken: session.accessToken,
        refreshToken: session.refreshToken,
        user: session.user,
      },
      remember,
    )
    accessToken.value = session.accessToken
    user.value = session.user
    return session.user
  }

  /**
   * Backend không có bước chờ Admin duyệt: tài khoản dùng được ngay, nhưng
   * không tự đăng nhập — trả về bản ghi vừa tạo để màn hình sau hiển thị.
   */
  async function register(payload: RegisterPayload): Promise<Member> {
    return authService.register(payload)
  }

  /**
   * Gọi backend thu hồi refresh token trước, rồi mới dọn máy. Backend hỏng
   * thì vẫn đăng xuất ở phía client — không giữ người dùng lại vì lỗi mạng.
   */
  async function logout(): Promise<void> {
    try {
      if (accessToken.value) await authService.logout()
    } catch {
      /* thu hồi phía server thất bại — token vẫn tự hết hạn */
    }
    clearLocal()
  }

  /**
   * Đồng bộ lại từ storage khi app khởi động hoặc khi người dùng F5.
   *
   * Không gọi API: backend chưa có endpoint kiểu GET /auth/me để lấy lại hồ
   * sơ từ token. Token thật sự còn hiệu lực hay không thì request đầu tiên
   * sẽ biết — http.ts tự làm mới, hết đường mới xoá phiên.
   */
  function restore(): void {
    accessToken.value = getAccessToken()
    user.value = getStoredUser()
    // Chỉ còn một nửa phiên (bị xoá tay, hoặc storage lỗi) — coi như đăng xuất.
    if (accessToken.value === null || user.value === null) clearLocal()
  }

  return {
    user,
    accessToken,
    hasToken,
    isAuthenticated,
    role,
    hasRole,
    login,
    register,
    logout,
    restore,
  }
})
