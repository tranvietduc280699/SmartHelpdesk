import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { requestService } from '@/services/requestService'
import { ApiError } from '@/services/http'
import type { Alert } from '@/types/request'
export const useAlertStore = defineStore('alerts', () => {
  const items = ref<Alert[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  let version = 0
  const unreadCount = computed(
    () => items.value.filter((alert) => !alert.isRead).length,
  )
  async function load(silent = false): Promise<void> {
    const current = ++version
    if (!silent) loading.value = true
    error.value = null
    try {
      const data = await requestService.listAlerts()
      if (current === version) items.value = data
    } catch (cause) {
      if (current === version)
        error.value =
          cause instanceof ApiError
            ? cause.message
            : 'Không tải được thông báo.'
    } finally {
      if (current === version) loading.value = false
    }
  }
  async function markRead(id: string): Promise<void> {
    const alert = items.value.find((item) => item.id === id)
    if (!alert || alert.isRead) return
    const current = version
    alert.isRead = true
    try {
      await requestService.markAlertRead(id)
    } catch (cause) {
      if (current === version) {
        alert.isRead = false
        error.value =
          cause instanceof ApiError
            ? cause.message
            : 'Không đánh dấu được thông báo.'
      }
    }
  }
  function reset(): void {
    version++
    items.value = []
    error.value = null
    loading.value = false
  }
  return { items, loading, error, unreadCount, load, markRead, reset }
})
