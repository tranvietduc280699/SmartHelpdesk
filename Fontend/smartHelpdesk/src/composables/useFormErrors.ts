import { computed, ref } from 'vue'

/**
 * Quản lý lỗi theo field cho một form.
 *
 * Thay cho kiểu cũ `err: { email: false, password: false }` — giờ giá trị là
 * *câu thông báo*, nên mỗi field hiện được đúng lỗi của nó thay vì một chuỗi
 * cố định viết cứng trong template.
 *
 *   const { errors, set, clear, reset, hasErrors } = useFormErrors<'email' | 'password'>()
 */
export function useFormErrors<K extends string>() {
  const errors = ref<Partial<Record<K, string>>>({})

  const hasErrors = computed(() => Object.keys(errors.value).length > 0)

  function set(field: K, message: string): void {
    errors.value = { ...errors.value, [field]: message }
  }

  /** Gộp nhiều lỗi cùng lúc, ví dụ fieldErrors do backend trả về. */
  function merge(map: Partial<Record<K, string>>): void {
    errors.value = { ...errors.value, ...map }
  }

  function clear(field: K): void {
    if (errors.value[field] === undefined) return
    const next = { ...errors.value }
    delete next[field]
    errors.value = next
  }

  function reset(): void {
    errors.value = {}
  }

  /** Trả về field lỗi đầu tiên theo thứ tự truyền vào — dùng để focus. */
  function firstInvalid(order: readonly K[]): K | null {
    return order.find((field) => errors.value[field] !== undefined) ?? null
  }

  return { errors, hasErrors, set, merge, clear, reset, firstInvalid }
}
