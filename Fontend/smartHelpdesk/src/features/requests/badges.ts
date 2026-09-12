/**
 * Ánh xạ giá trị nghiệp vụ sang màu của AppBadge.
 *
 * Để riêng khỏi types/request.ts: bên đó là hình dạng dữ liệu dùng chung
 * với backend, còn đây là chuyện trình bày, chỉ giao diện mới cần.
 */
import type {
  RequestCategory,
  RequestPriority,
  RequestStatus,
} from '@/types/request'

type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger'

export const STATUS_TONE: Record<RequestStatus, Tone> = {
  PENDING: 'warning',
  IN_PROGRESS: 'info',
  DONE: 'success',
}

/** Chỉ mức Cao mới tô đỏ — tô hết thì chẳng còn gì nổi bật. */
export const PRIORITY_TONE: Record<RequestPriority, Tone> = {
  HIGH: 'danger',
  MEDIUM: 'warning',
  LOW: 'neutral',
}

export const CATEGORY_TONE: Record<RequestCategory, Tone> = {
  BUG: 'danger',
  FEATURE: 'info',
  INQUIRY: 'neutral',
}
