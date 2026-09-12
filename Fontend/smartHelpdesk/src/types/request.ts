import type { AuthUser } from './user'

export type RequestCategory = 'BUG' | 'FEATURE' | 'INQUIRY'
export type RequestPriority = 'HIGH' | 'MEDIUM' | 'LOW'
export type RequestStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE'

/** Cột `action` của request_histories. */
export type HistoryAction = 'CREATE' | 'ASSIGN' | 'UPDATE' | 'CLOSE' | 'REOPEN'

export type AlertType =
  'ASSIGNED' | 'STATUS_CHANGED' | 'HIGH_PRIORITY_REGISTERED'

export interface SupportRequest {
  id: string
  companyId?: string | null
  companyName?: string | null
  companyAddress?: string | null
  companyPhone?: string | null
  title: string
  description: string
  category: RequestCategory
  priority: RequestPriority
  status: RequestStatus
  /** Người tạo yêu cầu. */
  client: AuthUser
  /** Người được phân công triển khai; null khi chưa giao. */
  assignedDeveloper: AuthUser | null
  createdAt: string
  updatedAt: string
}

export interface RequestHistory {
  id: string
  requestId: string
  changedBy: Pick<AuthUser, 'id' | 'name'>
  action: HistoryAction
  /** null với action CREATE — chưa có trạng thái trước đó. */
  fromStatus: RequestStatus | null
  toStatus: RequestStatus | null
  changedAt: string
  memo: string | null
}

export interface Alert {
  id: string
  requestId: string
  /** Tiêu đề request, để hiện được thông báo mà không phải gọi thêm API. */
  requestTitle?: string
  alertType: AlertType
  message: string
  isRead: boolean
  createdAt: string
}

/** Thống kê cho dashboard — `GET /api/requests/stats`. */
export interface RequestStats {
  total: number
  completed: number
  byStatus?: Record<RequestStatus, number>
  byCategory?: Record<RequestCategory, number>
  /** Tỉ lệ hoàn thành, 0–1. */
  completionRate: number
  byDeveloper: {
    developer: Pick<AuthUser, 'id' | 'name'>
    assigned: number
    done: number
    pending: number
    inProgress: number
  }[]
}

/**
 * Một trang dữ liệu. Hình dạng theo `Page` của Spring Data vì backend là
 * Spring Boot — nếu bên BE trả khác thì sửa ở đây và ở requestService.
 */
export interface Page<T> {
  content: T[]
  /** Đánh số từ 0, giống Spring. */
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/* ------------------------- Nhãn hiển thị tiếng Việt ------------------------- */

export const CATEGORY_LABEL: Record<RequestCategory, string> = {
  BUG: 'Lỗi',
  FEATURE: 'Tính năng',
  INQUIRY: 'Hỏi đáp',
}

export const PRIORITY_LABEL: Record<RequestPriority, string> = {
  HIGH: 'Cao',
  MEDIUM: 'Trung bình',
  LOW: 'Thấp',
}

export const STATUS_LABEL: Record<RequestStatus, string> = {
  PENDING: 'Chờ xử lý',
  IN_PROGRESS: 'Đang xử lý',
  DONE: 'Hoàn thành',
}

export const ACTION_LABEL: Record<HistoryAction, string> = {
  CREATE: 'Tạo yêu cầu',
  ASSIGN: 'Phân công',
  UPDATE: 'Cập nhật thông tin',
  CLOSE: 'Đóng yêu cầu',
  REOPEN: 'Mở lại',
}

/* ---------------------------- Luật nghiệp vụ ---------------------------- */

/**
 * Luật chuyển trạng thái, chép đúng từ đề bài (Step 3, mục 3):
 *   Cho phép:  PENDING → IN_PROGRESS → DONE
 *   Cấm:       DONE → bất kỳ trạng thái nào khác
 *   Cấm:       PENDING → DONE (nhảy cóc, bỏ qua IN_PROGRESS)
 *
 * Backend mới là nơi thực thi luật này; phía giao diện chỉ dùng để ẩn bớt
 * lựa chọn sai, tránh cho người dùng bấm rồi mới ăn lỗi.
 */
const ALLOWED_TRANSITIONS: Record<RequestStatus, RequestStatus[]> = {
  PENDING: ['IN_PROGRESS'],
  IN_PROGRESS: ['DONE'],
  DONE: [],
}

export function nextStatuses(current: RequestStatus): RequestStatus[] {
  return ALLOWED_TRANSITIONS[current]
}

export function canTransition(from: RequestStatus, to: RequestStatus): boolean {
  return ALLOWED_TRANSITIONS[from].includes(to)
}
