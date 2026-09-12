<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppModal from '@/components/ui/AppModal.vue'
import ChatPanel from './ChatPanel.vue'
import { ApiError } from '@/services/http'
import { chatService, type ChatMessage } from '@/services/chatService'
import { useAuthStore } from '@/stores/auth'
import { fromNow } from '@/utils/datetime'

const auth = useAuthStore()
const messages = ref<ChatMessage[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const open = ref(false)
const root = ref<HTMLElement | null>(null)
const selectedRequestId = ref<string | null>(null)
let timer: ReturnType<typeof setInterval> | undefined

const incoming = computed(() => messages.value.filter((message) => message.senderId !== auth.user?.id))
const unreadCount = computed(
  () => incoming.value.filter((message) => !message.isRead).length,
)
const threads = computed(() => {
  const groups = new Map<string, ChatMessage[]>()
  for (const message of messages.value) {
    const group = groups.get(message.requestId) ?? []
    group.push(message)
    groups.set(message.requestId, group)
  }
  return [...groups.entries()]
    .map(([requestId, threadMessages]) => {
      const sorted = [...threadMessages].sort((left, right) =>
        right.createdAt.localeCompare(left.createdAt),
      )
      return {
        requestId,
        title: sorted[0]?.requestTitle || 'Cuộc trò chuyện',
        latest: sorted[0]!,
        count: threadMessages.length,
        unread: threadMessages.filter(
          (message) => !message.isRead && message.senderId !== auth.user?.id,
        ).length,
      }
    })
    .sort((left, right) =>
      right.latest.createdAt.localeCompare(left.latest.createdAt),
    )
})

async function load(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    messages.value = await chatService.inbox()
  } catch (cause) {
    error.value =
      cause instanceof ApiError ? cause.message : 'Không tải được tin nhắn.'
  } finally {
    loading.value = false
  }
}

function onDocumentPointerDown(event: PointerEvent): void {
  if (open.value && !root.value?.contains(event.target as Node)) open.value = false
}

async function openThread(requestId: string): Promise<void> {
  open.value = false
  selectedRequestId.value = requestId
  const unread = messages.value.filter(
    (message) =>
      message.requestId === requestId &&
      !message.isRead &&
      message.senderId !== auth.user?.id,
  )
  await Promise.all(
    unread.map(async (message) => {
      try {
        await chatService.markRead(message.id)
        message.isRead = true
      } catch {
        // Keep the unread marker if the server could not update it.
      }
    }),
  )
}

function closeThread(): void {
  selectedRequestId.value = null
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') {
    open.value = false
    if (selectedRequestId.value) {
      selectedRequestId.value = null
    }
  }
}

function toggle(): void {
  open.value = !open.value
  if (open.value) void load()
}

onMounted(() => {
  void load()
  timer = setInterval(() => void load(), 5_000)
  document.addEventListener('pointerdown', onDocumentPointerDown)
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  document.removeEventListener('pointerdown', onDocumentPointerDown)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div ref="root" class="inbox-wrap">
    <button
      type="button"
      class="inbox-button"
      :aria-expanded="open"
      aria-haspopup="true"
      :aria-label="unreadCount ? `Tin nhắn, ${unreadCount} chưa đọc` : 'Tin nhắn'"
      @click="toggle"
    >
      <AppIcon name="message-circle" :size="19" />
      <span v-if="unreadCount" class="inbox-count">
        {{ unreadCount > 9 ? '9+' : unreadCount }}
      </span>
    </button>

    <div v-if="open" class="inbox-panel" role="dialog" aria-label="Tin nhắn">
      <header class="inbox-head">
        <strong>Tin nhắn</strong>
        <span>{{ threads.length }} cuộc trò chuyện</span>
      </header>
      <p v-if="loading" class="inbox-state">Đang tải…</p>
      <p v-else-if="error" class="inbox-state" role="alert">{{ error }}</p>
      <p v-else-if="!threads.length" class="inbox-state">Chưa có cuộc trò chuyện nào.</p>
      <ul v-else class="inbox-list">
        <li v-for="thread in threads" :key="thread.requestId">
          <button
            type="button"
            class="inbox-item"
            :class="{ 'inbox-item--unread': thread.unread > 0 }"
            @click="openThread(thread.requestId)"
          >
            <span class="inbox-item__icon"><AppIcon name="message-circle" :size="15" /></span>
            <span class="inbox-item__body">
              <strong>{{ thread.title }}</strong>
              <span class="inbox-item__request">{{ thread.count }} tin nhắn</span>
              <span class="inbox-item__text">{{ thread.latest.message }}</span>
              <time>{{ fromNow(thread.latest.createdAt) }}</time>
            </span>
            <span v-if="thread.unread" class="inbox-dot">{{ thread.unread }}</span>
          </button>
        </li>
      </ul>
    </div>

    <AppModal
      :open="selectedRequestId !== null"
      title="Cuộc trò chuyện"
      :width="760"
      @close="closeThread"
    >
      <ChatPanel
        v-if="selectedRequestId"
        :key="selectedRequestId"
        :request-id="selectedRequestId"
      />
    </AppModal>
  </div>
</template>

<style scoped>
.inbox-wrap { position: relative; }
.inbox-button {
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
}
.inbox-button:hover { background: var(--slate-100); color: var(--navy-900); }
.inbox-button:focus-visible { outline: none; box-shadow: var(--ring); }
.inbox-count {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 19px;
  height: 19px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--blue-700);
  color: var(--white);
  font-size: 11px;
  font-weight: 700;
  display: grid;
  place-items: center;
  border: 2px solid var(--white);
}
.inbox-panel {
  position: absolute;
  right: 0;
  top: calc(100% + 10px);
  width: min(390px, calc(100vw - 32px));
  max-height: 480px;
  overflow: hidden;
  background: var(--white);
  border: 1px solid var(--slate-200);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  z-index: 30;
}
.inbox-head { display: flex; justify-content: space-between; padding: 13px 15px; border-bottom: 1px solid var(--slate-200); color: var(--navy-900); }
.inbox-head span { color: var(--blue-700); font-size: 12px; }
.inbox-state { margin: 0; padding: 28px 15px; text-align: center; color: var(--slate-500); font-size: 13.5px; }
.inbox-list { list-style: none; margin: 0; padding: 0; max-height: 420px; overflow-y: auto; }
.inbox-item { width: 100%; display: flex; gap: 10px; padding: 12px 14px; border: 0; border-bottom: 1px solid var(--slate-100); background: transparent; text-align: left; font: inherit; cursor: pointer; }
.inbox-item:hover, .inbox-item--unread { background: var(--blue-50); }
.inbox-item__icon { flex: 0 0 auto; color: var(--blue-700); }
.inbox-item__body { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.inbox-item__body strong { color: var(--navy-900); font-size: 13px; }
.inbox-item__request { color: var(--blue-700); font-size: 12px; font-weight: 600; }
.inbox-item__text { overflow: hidden; color: var(--slate-600); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.inbox-item time { color: var(--slate-500); font-size: 11.5px; }
.inbox-dot { min-width: 18px; height: 18px; flex: 0 0 auto; margin: 5px 0 0 auto; padding: 0 4px; border-radius: 999px; background: var(--blue-700); color: var(--white); font-size: 10px; font-weight: 700; display: grid; place-items: center; }
</style>
