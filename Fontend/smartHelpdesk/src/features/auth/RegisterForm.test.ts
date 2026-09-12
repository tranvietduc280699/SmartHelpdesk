import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import RegisterForm from './RegisterForm.vue'
import { authService } from '@/services/authService'
import { companyService } from '@/services/companyService'
import { ApiError } from '@/services/http'
vi.mock('@/services/authService', () => ({
  authService: { register: vi.fn() },
}))
vi.mock('@/services/companyService', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/services/companyService')>()),
  companyService: { list: vi.fn() },
}))
const listCompanies = vi.mocked(companyService.list)
const register = vi.mocked(authService.register)
let wrapper: ReturnType<typeof mount>
beforeEach(async () => {
  listCompanies.mockReset()
  listCompanies.mockResolvedValue([
    { companyId: 'KR_SAMSUNG', companyName: 'Company from API' },
    { companyId: 'KR_NAVER', companyName: 'Second company' },
  ])
  register.mockReset()
  localStorage.clear()
  sessionStorage.clear()
  wrapper = mount(RegisterForm, { global: { plugins: [createPinia()] } })
  await flushPromises()
})
afterEach(() => wrapper.unmount())
async function fill() {
  await wrapper.get('#reg-name').setValue('API test')
  await wrapper.get('#reg-email').setValue('test@example.invalid')
  await wrapper.get('#reg-password').setValue('test-password')
  await wrapper.get('#reg-confirm').setValue('test-password')
}
describe('registration UI', () => {
  it('omits the test company and internal company from client choices', async () => {
    wrapper.unmount()
    listCompanies.mockResolvedValue([
      { companyId: 'CHAT_TEST_COMPANY', companyName: 'Chat Test Company' },
      { companyId: 'KR_BZCOM', companyName: 'KR_BZCOM' },
      { companyId: 'KR_SAMSUNG', companyName: 'Samsung C&T Corporation' },
    ])
    wrapper = mount(RegisterForm, { global: { plugins: [createPinia()] } })
    await flushPromises()
    expect(
      wrapper
        .findAll('#reg-company option')
        .map((option) => option.attributes('value')),
    ).toEqual(['', 'KR_SAMSUNG'])
    await fill()
    await wrapper.get('#reg-company').setValue('KR_SAMSUNG')
    await wrapper.get('form').trigger('submit')
    expect(register).toHaveBeenCalledWith(
      expect.objectContaining({ role: 'client', companyId: 'KR_SAMSUNG' }),
    )
  })
  it('shows a retry action and blocks client signup if company loading fails', async () => {
    listCompanies.mockRejectedValueOnce(new Error('Unavailable'))
    wrapper.unmount()
    wrapper = mount(RegisterForm, { global: { plugins: [createPinia()] } })
    await flushPromises()
    expect(
      wrapper.get('button[type="submit"]').attributes('disabled'),
    ).toBeDefined()
    expect(wrapper.text()).not.toContain('Company from API')
    listCompanies.mockResolvedValueOnce([
      { companyId: 'KR_SAMSUNG', companyName: 'Company from API' },
    ])
    const retry = wrapper
      .findAll('button')
      .find((button) => button.text() === 'Thử lại')!
    await retry.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Company from API')
  })
  it('blocks client registration until a company is selected', async () => {
    await fill()
    await wrapper.get('form').trigger('submit')
    expect(register).not.toHaveBeenCalled()
    expect(wrapper.get('#reg-company').attributes('required')).toBeDefined()
    expect(wrapper.get('#reg-company').attributes('aria-invalid')).toBe('true')
    expect(
      wrapper
        .findAll('#reg-company option')
        .map((option) => option.attributes('value')),
    ).toEqual(['', 'KR_SAMSUNG', 'KR_NAVER'])
  })
  it('does not retain the client company when registering as developer', async () => {
    await fill()
    await wrapper.get('#reg-company').setValue('KR_SAMSUNG')
    await wrapper.get('input[value="dev"]').setValue(true)
    expect(wrapper.find('#reg-company').exists()).toBe(false)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(register).toHaveBeenCalledWith(
      expect.objectContaining({ role: 'dev', companyId: 'KR_BZCOM' }),
    )
  })
  it('sends the selected company ID without internal DB fields', async () => {
    register.mockResolvedValue({
      id: 'test',
      name: 'API test',
      email: 'test@example.invalid',
      role: 'client',
      companyId: 'KR_SAMSUNG',
      phone: null,
      status: 'active',
      createdAt: null,
    })
    await fill()
    await wrapper.get('#reg-company').setValue('KR_SAMSUNG')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(register).toHaveBeenCalledWith({
      name: 'API test',
      email: 'test@example.invalid',
      password: 'test-password',
      role: 'client',
      companyId: 'KR_SAMSUNG',
      phone: null,
    })
    expect(wrapper.emitted('registered')).toHaveLength(1)
    expect(wrapper.find('input[value="admin"]').exists()).toBe(false)
  })
  it('maps backend companyId validation to the company input', async () => {
    register.mockRejectedValue(
      new ApiError('Invalid company', 400, 'VAL_400', {
        companyId: 'Unknown company',
      }),
    )
    await fill()
    await wrapper.get('#reg-company').setValue('KR_SAMSUNG')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('#reg-company').attributes('aria-invalid')).toBe('true')
    expect(wrapper.text()).toContain('Unknown company')
  })
})
