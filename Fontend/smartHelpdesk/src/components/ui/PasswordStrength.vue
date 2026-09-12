<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  /** Điểm 0–4, lấy từ passwordScore() trong utils/validators. */
  score: 0 | 1 | 2 | 3 | 4
}>()

const LEVELS = [
  { label: 'Độ mạnh mật khẩu', tone: 'none' },
  { label: 'Yếu', tone: 'weak' },
  { label: 'Trung bình', tone: 'fair' },
  { label: 'Khá', tone: 'good' },
  { label: 'Mạnh', tone: 'strong' },
] as const

const level = computed(() => LEVELS[props.score])
</script>

<template>
  <div class="pw-strength">
    <div class="pw-meter" aria-hidden="true">
      <span
        v-for="bar in 4"
        :key="bar"
        :class="bar <= score ? `is-filled tone-${level.tone}` : ''"
      ></span>
    </div>
    <!-- aria-live để người dùng bàn phím/đọc màn hình biết mức độ thay đổi -->
    <p class="pw-label" :class="`tone-${level.tone}`" aria-live="polite">
      {{ level.label }}
    </p>
  </div>
</template>

<style scoped>
.pw-meter {
  display: flex;
  gap: 5px;
  margin-top: 8px;
}

.pw-meter span {
  height: 4px;
  flex: 1;
  border-radius: 3px;
  background: var(--slate-200);
  transition: background var(--dur) ease;
}

.pw-label {
  font-size: 12px;
  margin: 5px 0 0;
  font-weight: 500;
  color: var(--slate-500);
}

.tone-weak {
  color: var(--red-600);
}
.tone-fair {
  color: var(--amber-600);
}
.tone-good {
  color: var(--blue-600);
}
.tone-strong {
  color: var(--green-600);
}

.pw-meter span.is-filled.tone-weak {
  background: var(--red-600);
}
.pw-meter span.is-filled.tone-fair {
  background: var(--amber-600);
}
.pw-meter span.is-filled.tone-good {
  background: var(--blue-600);
}
.pw-meter span.is-filled.tone-strong {
  background: var(--green-600);
}
</style>
