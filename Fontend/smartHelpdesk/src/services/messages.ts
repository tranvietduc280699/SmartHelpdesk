/**
 * Dịch thông báo của backend sang tiếng Việt.
 *
 * Backend trả message bằng tiếng Anh (constant/MessageConstants.java) và
 * chưa có file i18n nào — MessageServiceImpl tra MessageSource không thấy
 * thì trả nguyên chuỗi khoá. Giao diện thì hoàn toàn tiếng Việt, nên đối
 * chiếu ở đây thay vì hiển thị thẳng chuỗi tiếng Anh cho người dùng.
 *
 * Chuỗi nào chưa có trong bảng thì giữ nguyên: thà hiện tiếng Anh còn hơn
 * nuốt mất thông tin. Khi backend bổ sung messages_vi.properties thì xoá
 * file này và bỏ lời gọi trong http.ts.
 */

/** Khoá là message y hệt backend gửi về. */
const DICTIONARY: Record<string, string> = {
  // Thành công
  'Login successful': 'Đăng nhập thành công.',
  'Logout successful': 'Đã đăng xuất.',
  'Member registered successfully': 'Đăng ký thành công.',
  'Token refreshed successfully': 'Đã làm mới phiên đăng nhập.',

  // Lỗi nghiệp vụ
  'Invalid email or password': 'Email hoặc mật khẩu không đúng.',
  'Email is already registered': 'Email này đã được đăng ký.',
  'Access denied': 'Bạn không có quyền thực hiện thao tác này.',
  'Resource not found': 'Không tìm thấy dữ liệu.',
  'Bad request': 'Yêu cầu không hợp lệ.',
  'Invalid input parameters': 'Dữ liệu nhập vào không hợp lệ.',
  'System internal error': 'Máy chủ gặp sự cố. Vui lòng thử lại sau.',
  'An unexpected error occurred': 'Đã xảy ra lỗi không mong muốn.',
  'Refresh token cannot be blank': 'Thiếu refresh token.',
  'Refresh token is invalid or expired':
    'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',

  // Lỗi theo từng field (jakarta.validation)
  'Email cannot be blank': 'Vui lòng nhập email.',
  'Email must be a valid email format': 'Email không hợp lệ.',
  'Password cannot be blank': 'Vui lòng nhập mật khẩu.',
  'Name cannot be blank': 'Vui lòng nhập họ và tên.',
  'Role cannot be null': 'Vui lòng chọn vai trò.',
}

/** Thông báo ghép động: MessageConstants.MEMBER_NOT_FOUND + id. */
const PREFIXES: [string, string][] = [
  ['Member not found with ID:', 'Không tìm thấy thành viên.'],
]

export function viMessage(message: string): string {
  const text = message.trim()
  if (!text) return message

  const exact = DICTIONARY[text]
  if (exact) return exact

  for (const [prefix, translated] of PREFIXES) {
    if (text.startsWith(prefix)) return translated
  }

  /*
   * Lỗi validate nhiều field được GlobalExceptionHandler nối bằng ", ".
   * Dịch từng vế, và chỉ dùng bản dịch khi hiểu được hết — dịch nửa vời
   * sẽ ra câu lẫn hai thứ tiếng.
   */
  const parts = text.split(', ')
  if (parts.length > 1) {
    const translated = parts.map((part) => DICTIONARY[part.trim()])
    if (translated.every((part): part is string => part !== undefined)) {
      return translated.join(' ')
    }
  }

  return message
}
