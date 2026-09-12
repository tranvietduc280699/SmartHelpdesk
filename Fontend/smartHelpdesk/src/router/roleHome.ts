import type { UserRole } from '@/types/user'

/**
 * Trang mặc định sau khi đăng nhập, theo vai trò.
 *
 * Để riêng khỏi router/index.ts vì component (LoginForm) cũng cần dùng —
 * import thẳng từ router/index.ts sẽ tạo phụ thuộc vòng.
 */
export const ROLE_HOME: Record<UserRole, string> = {
  client: '/client',
  dev: '/dev',
  admin: '/admin',
}

export const ALL_ROLES: UserRole[] = ['client', 'dev', 'admin']
