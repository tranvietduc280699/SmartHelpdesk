<script setup lang="ts">
withDefaults(
  defineProps<{
    type?: 'button' | 'submit' | 'reset'
    variant?: 'primary' | 'ghost'
    /** Hiện spinner và khoá nút. */
    loading?: boolean
    disabled?: boolean
    /** Chiếm trọn chiều ngang. */
    block?: boolean
  }>(),
  { type: 'button', variant: 'primary' },
)
</script>

<template>
  <button
    :type="type"
    class="btn"
    :class="[`btn--${variant}`, { 'is-block': block }]"
    :disabled="disabled || loading"
    :aria-busy="loading || undefined"
  >
    <span v-if="loading" class="spinner" aria-hidden="true"></span>
    <span><slot /></span>
  </button>
</template>

<style scoped>
.btn {
  height: 50px;
  padding: 0 20px;
  border: 0;
  border-radius: var(--radius-sm);
  font: inherit;
  font-weight: 700;
  font-size: 15.5px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  transition:
    background var(--dur) ease,
    transform 60ms ease;
}

.btn.is-block {
  width: 100%;
}

.btn:active:not(:disabled) {
  transform: translateY(1px);
}

.btn:focus-visible {
  outline: none;
  box-shadow: var(--ring);
}

.btn--primary {
  background: var(--blue-700);
  color: var(--white);
}

.btn--primary:hover:not(:disabled) {
  background: var(--blue-600);
}

.btn--ghost {
  background: transparent;
  color: var(--slate-600);
  border: 1px solid var(--slate-200);
}

.btn--ghost:hover:not(:disabled) {
  background: var(--slate-100);
  color: var(--navy-900);
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.spinner {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2.5px solid rgba(255, 255, 255, 0.45);
  border-top-color: var(--white);
  animation: spin 0.7s linear infinite;
  flex: 0 0 auto;
}

.btn--ghost .spinner {
  border-color: var(--slate-200);
  border-top-color: var(--slate-600);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
