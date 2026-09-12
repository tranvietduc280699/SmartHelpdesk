import { describe, expect, it } from 'vitest'
import { canTransition, nextStatuses } from './request'

/**
 * Luật chép nguyên từ đề bài Bzcom, Step 3 mục 3:
 *   Cho phép:  PENDING → IN_PROGRESS → DONE
 *   Cấm:       DONE → bất kỳ trạng thái nào
 *   Cấm:       PENDING → DONE (bỏ qua IN_PROGRESS)
 */
describe('luật chuyển trạng thái request', () => {
  it('cho phép đi đúng thứ tự PENDING → IN_PROGRESS → DONE', () => {
    expect(canTransition('PENDING', 'IN_PROGRESS')).toBe(true)
    expect(canTransition('IN_PROGRESS', 'DONE')).toBe(true)
  })

  it('cấm nhảy cóc từ PENDING thẳng sang DONE', () => {
    expect(canTransition('PENDING', 'DONE')).toBe(false)
  })

  it('cấm mọi đường lùi khỏi DONE', () => {
    expect(canTransition('DONE', 'IN_PROGRESS')).toBe(false)
    expect(canTransition('DONE', 'PENDING')).toBe(false)
    expect(nextStatuses('DONE')).toEqual([])
  })

  it('cấm lùi từ IN_PROGRESS về PENDING', () => {
    expect(canTransition('IN_PROGRESS', 'PENDING')).toBe(false)
  })

  it('cấm giữ nguyên trạng thái cũ', () => {
    expect(canTransition('PENDING', 'PENDING')).toBe(false)
    expect(canTransition('IN_PROGRESS', 'IN_PROGRESS')).toBe(false)
  })

  it('nextStatuses() chỉ đưa ra nước đi hợp lệ', () => {
    expect(nextStatuses('PENDING')).toEqual(['IN_PROGRESS'])
    expect(nextStatuses('IN_PROGRESS')).toEqual(['DONE'])
  })
})
