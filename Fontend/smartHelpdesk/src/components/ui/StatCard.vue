<script setup lang="ts">
import AppIcon from './AppIcon.vue'
import type { IconName } from './icons'

withDefaults(
  defineProps<{
    label: string
    value: string | number
    icon?: IconName
    tone?: 'neutral' | 'info' | 'success' | 'warning'
    /** Dòng phụ nhỏ bên dưới, ví dụ "trên tổng 12". */
    hint?: string
  }>(),
  { tone: 'neutral' },
)
</script>

<template>
  <div class="stat">
    <div class="stat__head">
      <span class="stat__label">{{ label }}</span>
      <span v-if="icon" class="stat__ico" :class="`stat__ico--${tone}`">
        <AppIcon :name="icon" :size="16" />
      </span>
    </div>
    <p class="stat__value">{{ value }}</p>
    <p v-if="hint" class="stat__hint">{{ hint }}</p>
  </div>
</template>

<style scoped>
.stat {
  background: var(--white);
  border: 1px solid var(--slate-200);
  border-radius: var(--radius);
  padding: 16px 18px;
}

.stat__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.stat__label {
  font-size: 13px;
  color: var(--slate-500);
  font-weight: 500;
}

.stat__ico {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
}

.stat__ico--neutral {
  background: var(--slate-100);
  color: var(--slate-600);
}

.stat__ico--info {
  background: var(--blue-50);
  color: var(--blue-700);
}

.stat__ico--success {
  background: var(--green-50);
  color: var(--green-700);
}

.stat__ico--warning {
  background: var(--amber-50);
  color: var(--amber-800);
}

.stat__value {
  margin: 10px 0 0;
  font-size: 27px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--navy-900);
  line-height: 1.1;
}

.stat__hint {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: var(--slate-500);
}
</style>
