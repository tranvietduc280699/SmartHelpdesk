<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'
import AppIcon from './AppIcon.vue'

const titleId = useId()

const props = withDefaults(
  defineProps<{
    open: boolean
    title: string
    /** Khoá không cho đóng — dùng khi đang gửi dữ liệu đi. */
    busy?: boolean
    width?: number
  }>(),
  { width: 560 },
)

const emit = defineEmits<{ close: [] }>()

const panel = ref<HTMLElement | null>(null)
/** Trả tiêu điểm về đúng chỗ người dùng đứng trước khi mở hộp thoại. */
let lastFocused: HTMLElement | null = null

function requestClose(): void {
  if (props.busy) return
  emit('close')
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') requestClose()
}

watch(
  () => props.open,
  async (open) => {
    if (open) {
      lastFocused = document.activeElement as HTMLElement | null
      document.body.style.overflow = 'hidden'
      document.addEventListener('keydown', onKeydown)
      await nextTick()
      // Ưu tiên ô nhập đầu tiên; không có thì lấy chính khung hộp thoại.
      const target = panel.value?.querySelector<HTMLElement>(
        'input, textarea, select, button:not([data-close])',
      )
      ;(target ?? panel.value)?.focus()
    } else {
      document.body.style.overflow = ''
      document.removeEventListener('keydown', onKeydown)
      lastFocused?.focus()
      lastFocused = null
    }
  },
)

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="open" class="backdrop" @mousedown.self="requestClose">
        <div
          ref="panel"
          class="modal"
          :style="{ maxWidth: `${width}px` }"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="titleId"
          tabindex="-1"
        >
          <header class="modal__head">
            <h2 :id="titleId">{{ title }}</h2>
            <button
              type="button"
              class="modal__close"
              data-close
              aria-label="Đóng"
              :disabled="busy"
              @click="requestClose"
            >
              <AppIcon name="x" :size="18" />
            </button>
          </header>

          <div class="modal__body">
            <slot />
          </div>

          <footer v-if="$slots.footer" class="modal__foot">
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 6vh 20px 40px;
  overflow-y: auto;
}

.modal {
  width: 100%;
  background: var(--white);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  outline: none;
}

.modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 14px;
  border-bottom: 1px solid var(--slate-200);
}

.modal__head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: var(--navy-900);
}

.modal__close {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--slate-500);
  cursor: pointer;
  flex: 0 0 auto;
  transition:
    background var(--dur) ease,
    color var(--dur) ease;
}

.modal__close:hover:not(:disabled) {
  background: var(--slate-100);
  color: var(--navy-900);
}

.modal__close:focus-visible {
  outline: none;
  box-shadow: var(--ring);
}

.modal__body {
  padding: 20px 22px;
}

.modal__foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 22px 20px;
}

/* Chuyển cảnh nhẹ; người bật "giảm chuyển động" thì tắt (xem base.css). */
.modal-enter-active,
.modal-leave-active {
  transition: opacity var(--dur) ease;
}

.modal-enter-active .modal,
.modal-leave-active .modal {
  transition: transform var(--dur) ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal,
.modal-leave-to .modal {
  transform: translateY(-8px);
}
</style>
