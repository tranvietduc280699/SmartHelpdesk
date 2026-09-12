export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * Số điện thoại Việt Nam: 10 số bắt đầu bằng 0, hoặc dạng +84 rồi 9 số.
 * Gọi kèm normalizePhone() để bỏ khoảng trắng/dấu chấm trước khi kiểm tra.
 */
export const PHONE_RE = /^(0\d{9}|\+84\d{9})$/

export const MIN_PASSWORD_LENGTH = 8

export function normalizePhone(value: string): string {
  return value.replace(/[\s.\-()]/g, '')
}

export function isEmail(value: string): boolean {
  return EMAIL_RE.test(value.trim())
}

export function isPhone(value: string): boolean {
  return PHONE_RE.test(normalizePhone(value.trim()))
}

/** Điểm độ mạnh mật khẩu từ 0 (rỗng) đến 4 (mạnh). */
export function passwordScore(value: string): 0 | 1 | 2 | 3 | 4 {
  if (!value) return 0
  let score = 0
  if (value.length >= MIN_PASSWORD_LENGTH) score++
  if (/[A-Z]/.test(value) && /[a-z]/.test(value)) score++
  if (/\d/.test(value)) score++
  if (/[^A-Za-z0-9]/.test(value)) score++
  return Math.min(score, 4) as 0 | 1 | 2 | 3 | 4
}
