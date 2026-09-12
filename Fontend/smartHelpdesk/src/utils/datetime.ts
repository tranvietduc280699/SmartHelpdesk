/** Định dạng thời gian cho giao diện tiếng Việt. */

const DATE_TIME = new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

const RELATIVE = new Intl.RelativeTimeFormat('vi', { numeric: 'auto' })

/** '06/08/2026, 09:00' */
export function formatDateTime(iso: string): string {
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? '—' : DATE_TIME.format(date)
}

const UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['year', 365 * 24 * 3600],
  ['month', 30 * 24 * 3600],
  ['day', 24 * 3600],
  ['hour', 3600],
  ['minute', 60],
]

/**
 * '2 giờ trước'. Dùng ở bảng danh sách vì người dùng quan tâm "mới hay cũ"
 * hơn là mốc chính xác; chỗ nào cần chính xác thì đặt formatDateTime vào
 * thuộc tính title để rê chuột xem.
 */
export function fromNow(iso: string, now: number = Date.now()): string {
  const time = new Date(iso).getTime()
  if (Number.isNaN(time)) return '—'

  const seconds = Math.round((time - now) / 1000)
  for (const [unit, size] of UNITS) {
    if (Math.abs(seconds) >= size) {
      return RELATIVE.format(Math.round(seconds / size), unit)
    }
  }
  return 'vừa xong'
}
