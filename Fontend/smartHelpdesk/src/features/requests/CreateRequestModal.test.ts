import { afterEach, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import CreateRequestModal from './CreateRequestModal.vue'
import { requestService } from '@/services/requestService'
vi.mock('@/services/requestService', () => ({
  requestService: { create: vi.fn(), suggest: vi.fn() },
}))
afterEach(() => {
  document.body.innerHTML = ''
  vi.clearAllMocks()
})
it('submits a title and category with optional description empty', async () => {
  vi.mocked(requestService.create).mockResolvedValue({
    id: 'test-request',
  } as Awaited<ReturnType<typeof requestService.create>>)
  const wrapper = mount(CreateRequestModal, {
    props: { open: true },
    global: { stubs: { teleport: true } },
  })
  await wrapper.get('#new-title').setValue('A')
  await wrapper.get('form').trigger('submit')
  await flushPromises()
  expect(requestService.create).toHaveBeenCalledWith({
    title: 'A',
    description: '',
    category: 'BUG',
    priority: 'MEDIUM',
  })
  expect(wrapper.emitted('created')).toHaveLength(1)
  wrapper.unmount()
})
