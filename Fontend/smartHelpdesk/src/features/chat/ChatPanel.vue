<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AppButton from '@/components/ui/AppButton.vue'
import FormBanner from '@/components/ui/FormBanner.vue'
import { ApiError } from '@/services/http'
import { chatService, type ChatMessage } from '@/services/chatService'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime, fromNow } from '@/utils/datetime'

const props = defineProps<{ requestId: string }>()

const auth = useAuthStore()
const messages = ref<ChatMessage[]>([])
const draft = ref('')
const loading = ref(true)
const sending = ref(false)
const error = ref<string | null>(null)
const refreshedAt = ref<string | null>(null)
let refreshTimer: ReturnType<typeof setInterval> | undefined

const canSend = computed(() => draft.value.trim().length > 0 && !sending.value)

function isMine(message: ChatMessage): boolean {
  return message.senderId === auth.user?.id
}

async function loadMessages(showLoading = false): Promise<void> {
  if (showLoading) loading.value = true
  error.value = null
  try {
    messages.value = await chatService.list(props.requestId)
    refreshedAt.value = new Date().toISOString()
  } catch (cause) {
    error.value =
      cause instanceof ApiError
        ? cause.message
        : 'Không tải được lịch sử trò chuyện.'
  } finally {
    loading.value = false
  }
}

async function send(): Promise<void> {
  const message = draft.value.trim()
  if (!message || sending.value) return
  sending.value = true
  error.value = null
  try {
    const created = await chatService.send(props.requestId, message)
    messages.value = [...messages.value, created].sort((left, right) =>
      left.createdAt.localeCompare(right.createdAt),
    )
    draft.value = ''
  } catch (cause) {
    error.value =
      cause instanceof ApiError ? cause.message : 'Không gửi được tin nhắn.'
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  void loadMessages(true)
  refreshTimer = setInterval(() => void loadMessages(), 5_000)
})

onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<template>
  <section id="chat" class="chat card">
    <div class="chat__head">
      <div>
        <h2>Trao đổi</h2>
        <p>ADMIN, Developer và Client cùng trao đổi trong request này.</p>
      </div>
      <span v-if="refreshedAt" class="chat__updated" :title="formatDateTime(refreshedAt)">
        Cập nhật {{ fromNow(refreshedAt) }}
      </span>
    </div>

    <FormBanner v-if="error">{{ error }}</FormBanner>

    <div v-if="loading" class="chat__state">Đang tải tin nhắn…</div>
    <div v-else-if="!messages.length" class="chat__state">
      Chưa có tin nhắn. Hãy bắt đầu trao đổi.
    </div>
    <div v-else class="chat__messages" aria-live="polite">
      <article
        v-for="message in messages"
        :key="message.id"
        class="message"
        :class="{ 'message--mine': isMine(message) }"
      >
        <div class="message__meta">
          <strong>{{ isMine(message) ? 'Bạn' : message.senderName || 'Thành viên' }}</strong>
          <span>{{ message.senderRole }}</span>
          <time :datetime="message.createdAt" :title="formatDateTime(message.createdAt)">
            {{ fromNow(message.createdAt) }}
          </time>
          <span v-if="!message.persisted" class="message__pending">Đang lưu</span>
        </div>
        <p>{{ message.message }}</p>
      </article>
    </div>

    <form class="chat__composer" @submit.prevent="send">
      <textarea
        v-model="draft"
        rows="3"
        maxlength="10000"
        placeholder="Nhập nội dung trao đổi…"
        :disabled="sending"
        @keydown.ctrl.enter.prevent="send"
      ></textarea>
      <AppButton type="submit" :loading="sending" :disabled="!canSend">
        Gửi tin nhắn
      </AppButton>
    </form>
  </section>
</template>

<style scoped>
.chat {
  grid-column: 1 / -1;
}

.chat__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.chat h2 {
  margin: 0 0 4px;
  font-size: 16px;
}

.chat__head p,
.chat__updated {
  margin: 0;
  color: var(--slate-500);
  font-size: 12.5px;
}

.chat__updated {
  white-space: nowrap;
}

.chat__state {
  padding: 24px 0;
  color: var(--slate-500);
  font-size: 14px;
  text-align: center;
}

.chat__messages {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 420px;
  overflow-y: auto;
  padding: 4px 2px 14px;
}

.message {
  align-self: flex-start;
  max-width: min(760px, 88%);
  padding: 10px 13px;
  border: 1px solid var(--slate-200);
  border-radius: 10px 10px 10px 3px;
  background: var(--slate-50);
}

.message--mine {
  align-self: flex-end;
  border-color: var(--blue-200);
  border-radius: 10px 10px 3px 10px;
  background: var(--blue-50);
}

.message__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  color: var(--slate-500);
  font-size: 11.5px;
}

.message__meta strong {
  color: var(--navy-800);
}

.message p {
  margin: 5px 0 0;
  color: var(--navy-900);
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.message__pending {
  color: var(--amber-700);
}

.chat__composer {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  border-top: 1px solid var(--slate-200);
  padding-top: 14px;
}

.chat__composer textarea {
  min-height: 78px;
  flex: 1;
  resize: vertical;
  border: 1px solid var(--slate-200);
  border-radius: 7px;
  padding: 10px 12px;
  color: var(--navy-900);
  font: inherit;
  line-height: 1.45;
}

.chat__composer textarea:focus {
  outline: none;
  border-color: var(--blue-500);
  box-shadow: var(--ring);
}

@media (max-width: 600px) {
  .chat__head,
  .chat__composer {
    align-items: stretch;
    flex-direction: column;
  }

  .chat__updated {
    white-space: normal;
  }

  .message {
    max-width: 96%;
  }
}
</style>
