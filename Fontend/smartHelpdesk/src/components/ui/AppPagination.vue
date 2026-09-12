<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps<{
  /** Đánh số từ 0, giống Spring Data. */
  page: number
  totalPages: number
  totalElements: number
  size: number
}>()

const emit = defineEmits<{ change: [page: number] }>()

const from = computed(() => props.page * props.size + 1)
const to = computed(() =>
  Math.min((props.page + 1) * props.size, props.totalElements),
)

/**
 * Dải số trang quanh trang hiện tại. Nhiều trang thì chèn dấu '…' để thanh
 * phân trang không dài vô tận.
 */
const pages = computed<(number | '…')[]>(() => {
  const total = props.totalPages
  const current = props.page
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)

  const around = [current - 1, current, current + 1].filter(
    (p) => p > 0 && p < total - 1,
  )
  const result: (number | '…')[] = [0]
  if (around[0] !== undefined && around[0] > 1) result.push('…')
  result.push(...around)
  if (around[around.length - 1]! < total - 2) result.push('…')
  result.push(total - 1)
  return result
})

function go(page: number): void {
  if (page < 0 || page >= props.totalPages || page === props.page) return
  emit('change', page)
}
</script>

<template>
  <nav v-if="totalElements > 0" class="pager" aria-label="Phân trang">
    <p class="pager__count">
      Hiện <strong>{{ from }}–{{ to }}</strong> trên
      <strong>{{ totalElements }}</strong> yêu cầu
    </p>

    <ul class="pager__list">
      <li>
        <button
          type="button"
          class="pager__btn"
          :disabled="page === 0"
          aria-label="Trang trước"
          @click="go(page - 1)"
        >
          <AppIcon name="chevron-left" :size="16" />
        </button>
      </li>

      <li v-for="(item, index) in pages" :key="`${item}-${index}`">
        <span v-if="item === '…'" class="pager__gap" aria-hidden="true">…</span>
        <button
          v-else
          type="button"
          class="pager__btn"
          :class="{ 'is-current': item === page }"
          :aria-current="item === page ? 'page' : undefined"
          @click="go(item)"
        >
          {{ item + 1 }}
        </button>
      </li>

      <li>
        <button
          type="button"
          class="pager__btn"
          :disabled="page >= totalPages - 1"
          aria-label="Trang sau"
          @click="go(page + 1)"
        >
          <AppIcon name="chevron-right" :size="16" />
        </button>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 14px 18px;
  border-top: 1px solid var(--slate-200);
}

.pager__count {
  margin: 0;
  font-size: 13px;
  color: var(--slate-500);
}

.pager__count strong {
  color: var(--navy-800);
  font-weight: 600;
}

.pager__list {
  display: flex;
  align-items: center;
  gap: 4px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.pager__btn {
  min-width: 34px;
  height: 34px;
  padding: 0 8px;
  display: grid;
  place-items: center;
  border: 1px solid var(--slate-200);
  border-radius: 8px;
  background: var(--white);
  color: var(--slate-600);
  font: inherit;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background var(--dur) ease,
    color var(--dur) ease,
    border-color var(--dur) ease;
}

.pager__btn:hover:not(:disabled):not(.is-current) {
  background: var(--slate-100);
  color: var(--navy-900);
}

.pager__btn:focus-visible {
  outline: none;
  box-shadow: var(--ring);
}

.pager__btn.is-current {
  background: var(--blue-700);
  border-color: var(--blue-700);
  color: var(--white);
  cursor: default;
}

.pager__btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.pager__gap {
  display: inline-block;
  min-width: 22px;
  text-align: center;
  color: var(--slate-400);
}

@media (max-width: 600px) {
  .pager {
    justify-content: center;
  }
}
</style>
