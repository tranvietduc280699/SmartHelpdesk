<script setup lang="ts">
withDefaults(
  defineProps<{
    tone?: 'neutral' | 'info' | 'success' | 'warning' | 'danger'
    /** Chấm tròn phía trước — dùng cho trạng thái, dễ quét mắt hơn. */
    dot?: boolean
  }>(),
  { tone: 'neutral' },
)
</script>

<template>
  <span class="badge" :class="`badge--${tone}`">
    <span v-if="dot" class="badge__dot" aria-hidden="true"></span>
    <slot />
  </span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1.5;
  white-space: nowrap;
  border: 1px solid transparent;
}

.badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex: 0 0 auto;
}

.badge--neutral {
  background: var(--slate-100);
  color: var(--slate-600);
  border-color: var(--slate-200);
}

.badge--info {
  background: var(--blue-50);
  color: var(--blue-700);
  border-color: var(--blue-100);
}

.badge--success {
  background: var(--green-50);
  color: var(--green-700);
  border-color: var(--green-200);
}

.badge--warning {
  background: var(--amber-50);
  color: var(--amber-800);
  border-color: var(--amber-200);
}

.badge--danger {
  background: var(--red-50);
  color: var(--red-700);
  border-color: var(--red-200);
}
</style>
