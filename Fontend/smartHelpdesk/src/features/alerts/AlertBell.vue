<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/ui/AppIcon.vue'
import { useAlertStore } from '@/stores/alerts'
import type { Alert } from '@/types/request'
import { fromNow } from '@/utils/datetime'

const alerts = useAlertStore()
const router = useRouter()

const open = ref(false)
const root = ref<HTMLElement | null>(null)
let refreshTimer: ReturnType<typeof setInterval> | undefined
let refreshing = false
async function refresh(silent = false): Promise<void> {
  if (refreshing) return
  refreshing = true
  try {
    await alerts.load(silent)
  } finally {
    refreshing = false
  }
}
function refreshVisible(): void {
  if (document.visibilityState !== 'hidden') void refresh(true)
}

/** Bấm ra ngoài hoặc bấm Esc thì đóng — hành vi quen thuộc của menu thả xuống. */
function onDocumentPointerDown(event: PointerEvent): void {
  if (!open.value) return
  if (!root.value?.contains(event.target as Node)) open.value = false
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') open.value = false
}

onMounted(() => {
  void refresh()
  refreshTimer = setInterval(refreshVisible, 10_000)
  document.addEventListener('visibilitychange', refreshVisible)
  window.addEventListener('focus', refreshVisible)
  window.addEventListener('online', refreshVisible)
  document.addEventListener('pointerdown', onDocumentPointerDown)
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  clearInterval(refreshTimer)
  document.removeEventListener('visibilitychange', refreshVisible)
  window.removeEventListener('focus', refreshVisible)
  window.removeEventListener('online', refreshVisible)
  document.removeEventListener('pointerdown', onDocumentPointerDown)
  document.removeEventListener('keydown', onKeydown)
})

function toggle(): void {
  open.value = !open.value
  if (open.value) void refresh()
}

async function openAlert(alert: Alert): Promise<void> {
  open.value = false
  await alerts.markRead(alert.id)
  await router.push({ name: 'request-detail', params: { id: alert.requestId } })
}

const ALERT_ICON = {
  ASSIGNED: 'user-check',
  STATUS_CHANGED: 'check-circle',
  HIGH_PRIORITY_REGISTERED: 'alert-triangle',
} as const
</script>

<template>
  <div ref="root" class="bell-wrap">
    <button
      type="button"
      class="bell"
      :aria-expanded="open"
      aria-haspopup="true"
      :aria-label="
        alerts.unreadCount > 0
          ? `Thông báo, ${alerts.unreadCount} chưa đọc`
          : 'Thông báo'
      "
      @click="toggle"
    >
      <AppIcon name="bell" :size="19" />
      <span v-if="alerts.unreadCount > 0" class="bell__count">
        {{ alerts.unreadCount > 9 ? '9+' : alerts.unreadCount }}
      </span>
    </button>

    <div v-if="open" class="panel" role="menu">
      <header class="panel__head">
        <span>Thông báo</span>
        <span v-if="alerts.unreadCount > 0" class="panel__unread">
          {{ alerts.unreadCount }} chưa đọc
        </span>
      </header>

      <p v-if="alerts.loading" class="panel__state">Đang tải…</p>
      <p v-else-if="alerts.error" class="panel__state" role="alert">
        {{ alerts.error }}
        <button type="button" class="link" @click="refresh()">Thử lại</button>
      </p>
      <p v-else-if="alerts.items.length === 0" class="panel__state">
        Chưa có thông báo nào.
      </p>

      <ul v-else class="panel__list">
        <li v-for="alert in alerts.items" :key="alert.id">
          <button
            type="button"
            class="item"
            :class="{ 'is-unread': !alert.isRead }"
            role="menuitem"
            @click="openAlert(alert)"
          >
            <span class="item__ico">
              <AppIcon :name="ALERT_ICON[alert.alertType]" :size="15" />
            </span>
            <span class="item__body">
              <span class="item__msg">{{ alert.message }}</span>
              <span class="item__time">{{ fromNow(alert.createdAt) }}</span>
            </span>
            <span v-if="!alert.isRead" class="item__dot" aria-hidden="true" />
          </button>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.bell-wrap {
  position: relative;
}

.bell {
  position: relative;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid var(--slate-200);
  border-radius: var(--radius-sm);
  background: var(--white);
  color: var(--slate-600);
  cursor: pointer;
  transition:
    background var(--dur) ease,
    color var(--dur) ease;
}

.bell:hover {
  background: var(--slate-100);
  color: var(--navy-900);
}

.bell:focus-visible {
  outline: none;
  box-shadow: var(--ring);
}

.bell__count {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 19px;
  height: 19px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--red-600);
  color: var(--white);
  font-size: 11px;
  font-weight: 700;
  display: grid;
  place-items: center;
  border: 2px solid var(--white);
}

.panel {
  position: absolute;
  right: 0;
  top: calc(100% + 10px);
  width: min(360px, calc(100vw - 32px));
  background: var(--white);
  border: 1px solid var(--slate-200);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  z-index: 30;
  overflow: hidden;
}

.panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--slate-200);
  font-size: 13.5px;
  font-weight: 700;
  color: var(--navy-900);
}

.panel__unread {
  font-size: 12px;
  font-weight: 600;
  color: var(--blue-700);
}

.panel__state {
  margin: 0;
  padding: 26px 14px;
  text-align: center;
  font-size: 13.5px;
  color: var(--slate-500);
}

.panel__list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 380px;
  overflow-y: auto;
}

.item {
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 11px;
  padding: 12px 14px;
  border: 0;
  border-bottom: 1px solid var(--slate-100);
  background: transparent;
  text-align: left;
  font: inherit;
  cursor: pointer;
  transition: background var(--dur) ease;
}

.item:hover {
  background: var(--slate-50);
}

.item:focus-visible {
  outline: none;
  box-shadow: inset var(--ring);
}

.item.is-unread {
  background: var(--blue-50);
}

.item.is-unread:hover {
  background: var(--blue-100);
}

.item__ico {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--slate-100);
  color: var(--slate-600);
  display: grid;
  place-items: center;
  flex: 0 0 auto;
}

.item__body {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.item__msg {
  font-size: 13.5px;
  line-height: 1.5;
  color: var(--navy-800);
}

.item__time {
  font-size: 12px;
  color: var(--slate-500);
}

.item__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--blue-700);
  flex: 0 0 auto;
  margin-top: 8px;
}
</style>
