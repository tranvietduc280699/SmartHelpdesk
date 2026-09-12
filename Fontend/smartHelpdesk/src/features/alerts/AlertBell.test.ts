import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { nextTick } from 'vue'
import AlertBell from './AlertBell.vue'
import { requestService } from '@/services/requestService'
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))
vi.mock('@/services/requestService', () => ({
  requestService: { listAlerts: vi.fn(), markAlertRead: vi.fn() },
}))
const list = vi.mocked(requestService.listAlerts)
let wrapper: ReturnType<typeof mount>
beforeEach(() => {
  vi.useFakeTimers()
  list.mockReset()
  list.mockResolvedValue([])
  vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('visible')
})
afterEach(() => {
  wrapper?.unmount()
  vi.useRealTimers()
  vi.restoreAllMocks()
})
async function settle() {
  await vi.advanceTimersByTimeAsync(0)
  await nextTick()
}
it('updates the badge without opening the bell and stops after unmount', async () => {
  wrapper = mount(AlertBell, { global: { plugins: [createPinia()] } })
  await settle()
  expect(list).toHaveBeenCalledTimes(1)
  list.mockResolvedValue([
    {
      id: 'alert-test',
      requestId: 'request-test',
      alertType: 'HIGH_PRIORITY_REGISTERED',
      message: 'New high priority request',
      isRead: false,
      createdAt: '2026-09-12T00:00:00Z',
    },
  ])
  await vi.advanceTimersByTimeAsync(10000)
  await nextTick()
  expect(wrapper.find('.bell__count').text()).toBe('1')
  expect(wrapper.find('.panel').exists()).toBe(false)
  wrapper.unmount()
  const calls = list.mock.calls.length
  await vi.advanceTimersByTimeAsync(20000)
  window.dispatchEvent(new Event('focus'))
  await settle()
  expect(list).toHaveBeenCalledTimes(calls)
})
it('pauses polling in hidden tabs and refreshes when the tab returns', async () => {
  wrapper = mount(AlertBell, { global: { plugins: [createPinia()] } })
  await settle()
  vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden')
  await vi.advanceTimersByTimeAsync(10000)
  expect(list).toHaveBeenCalledTimes(1)
  vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('visible')
  document.dispatchEvent(new Event('visibilitychange'))
  await settle()
  expect(list).toHaveBeenCalledTimes(2)
})
it('does not overlap refresh requests on timers or focus', async () => {
  let finish: (value: []) => void = () => {}
  list.mockImplementation(
    () =>
      new Promise((resolve) => {
        finish = resolve
      }),
  )
  wrapper = mount(AlertBell, { global: { plugins: [createPinia()] } })
  await vi.advanceTimersByTimeAsync(20000)
  window.dispatchEvent(new Event('focus'))
  expect(list).toHaveBeenCalledTimes(1)
  finish([])
  await settle()
  await vi.advanceTimersByTimeAsync(10000)
  expect(list).toHaveBeenCalledTimes(2)
  finish([])
  await settle()
})
