import { describe, expect, it } from 'vitest'
import { useFormErrors } from './useFormErrors'

type Field = 'email' | 'password' | 'name'

describe('useFormErrors', () => {
  it('bắt đầu ở trạng thái không lỗi', () => {
    const { errors, hasErrors } = useFormErrors<Field>()
    expect(errors.value).toEqual({})
    expect(hasErrors.value).toBe(false)
  })

  it('set() ghi thông báo và bật cờ hasErrors', () => {
    const { errors, hasErrors, set } = useFormErrors<Field>()
    set('email', 'Email không hợp lệ.')
    expect(errors.value.email).toBe('Email không hợp lệ.')
    expect(hasErrors.value).toBe(true)
  })

  it('clear() chỉ xoá đúng field được chỉ định', () => {
    const { errors, set, clear } = useFormErrors<Field>()
    set('email', 'lỗi email')
    set('password', 'lỗi mật khẩu')

    clear('email')

    expect(errors.value.email).toBeUndefined()
    expect(errors.value.password).toBe('lỗi mật khẩu')
  })

  it('reset() xoá sạch', () => {
    const { hasErrors, set, reset } = useFormErrors<Field>()
    set('email', 'lỗi')
    reset()
    expect(hasErrors.value).toBe(false)
  })

  it('merge() gộp lỗi từ backend, đè lên lỗi cùng field', () => {
    const { errors, set, merge } = useFormErrors<Field>()
    set('email', 'lỗi cũ')
    merge({ email: 'Email đã được đăng ký.', name: 'Tên bị trùng.' })

    expect(errors.value.email).toBe('Email đã được đăng ký.')
    expect(errors.value.name).toBe('Tên bị trùng.')
  })

  it('firstInvalid() trả field lỗi đầu tiên theo thứ tự truyền vào', () => {
    const { set, firstInvalid } = useFormErrors<Field>()
    set('password', 'lỗi mật khẩu')
    set('name', 'lỗi tên')

    expect(firstInvalid(['name', 'email', 'password'])).toBe('name')
    expect(firstInvalid(['email', 'password', 'name'])).toBe('password')
  })

  it('firstInvalid() trả null khi không có lỗi', () => {
    const { firstInvalid } = useFormErrors<Field>()
    expect(firstInvalid(['email', 'password'])).toBeNull()
  })
})
