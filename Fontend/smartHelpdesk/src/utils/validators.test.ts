import { describe, expect, it } from 'vitest'
import { isEmail, isPhone, normalizePhone, passwordScore } from './validators'

describe('isEmail', () => {
  it('chấp nhận email hợp lệ', () => {
    expect(isEmail('ban@congty.com')).toBe(true)
    expect(isEmail('  ban@congty.com  ')).toBe(true)
  })

  it('từ chối email thiếu phần bắt buộc', () => {
    expect(isEmail('')).toBe(false)
    expect(isEmail('ban@congty')).toBe(false)
    expect(isEmail('congty.com')).toBe(false)
    expect(isEmail('ban @congty.com')).toBe(false)
  })
})

describe('normalizePhone', () => {
  it('bỏ khoảng trắng và dấu phân cách', () => {
    expect(normalizePhone('0912 345 678')).toBe('0912345678')
    expect(normalizePhone('091.234.5678')).toBe('0912345678')
    expect(normalizePhone('(091) 234-5678')).toBe('0912345678')
  })
})

describe('isPhone', () => {
  it('chấp nhận số Việt Nam 10 chữ số', () => {
    expect(isPhone('0912345678')).toBe(true)
    expect(isPhone('0912 345 678')).toBe(true)
  })

  it('chấp nhận dạng +84 kèm 9 chữ số', () => {
    expect(isPhone('+84912345678')).toBe(true)
  })

  it('từ chối số quá dài — lỗi mà regex cũ bỏ lọt', () => {
    expect(isPhone('09123456789012')).toBe(false)
    expect(isPhone('+8491234567890')).toBe(false)
  })

  it('từ chối số quá ngắn hoặc sai đầu số', () => {
    expect(isPhone('091234567')).toBe(false)
    expect(isPhone('1912345678')).toBe(false)
    expect(isPhone('')).toBe(false)
  })
})

describe('passwordScore', () => {
  it('trả 0 khi rỗng', () => {
    expect(passwordScore('')).toBe(0)
  })

  it('cộng điểm theo từng tiêu chí', () => {
    expect(passwordScore('abcdefgh')).toBe(1) // đủ độ dài
    expect(passwordScore('abcdefgH')).toBe(2) // + hoa/thường
    expect(passwordScore('abcdefgH1')).toBe(3) // + chữ số
    expect(passwordScore('abcdefgH1!')).toBe(4) // + ký tự đặc biệt
  })

  it('không vượt quá 4', () => {
    expect(passwordScore('aB1!aB1!aB1!aB1!')).toBe(4)
  })
})
