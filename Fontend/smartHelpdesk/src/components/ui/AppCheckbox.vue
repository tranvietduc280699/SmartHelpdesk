<script setup lang="ts">
import AppIcon from './AppIcon.vue'

withDefaults(
  defineProps<{
    /** Cần khi form muốn focus vào ô này lúc báo lỗi. */
    id?: string
    /** 'start' khi nhãn dài nhiều dòng (ví dụ dòng điều khoản). */
    align?: 'center' | 'start'
    disabled?: boolean
  }>(),
  { align: 'center' },
)

const model = defineModel<boolean>({ required: true })
</script>

<template>
  <label class="check" :class="`check--${align}`">
    <input :id="id" v-model="model" type="checkbox" :disabled="disabled" />
    <span class="box" aria-hidden="true">
      <AppIcon name="check" :size="12" :stroke-width="3" />
    </span>
    <span><slot /></span>
  </label>
</template>

<style scoped>
.check {
  display: inline-flex;
  gap: 9px;
  font-size: 13.5px;
  color: var(--slate-600);
  cursor: pointer;
  user-select: none;
}

.check--center {
  align-items: center;
}

.check--start {
  align-items: flex-start;
  font-size: 13px;
}

.check--start .box {
  margin-top: 1px;
}

.check input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.box {
  width: 19px;
  height: 19px;
  border-radius: 6px;
  border: 1.5px solid var(--slate-200);
  background: var(--white);
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  color: var(--white);
  transition:
    border-color var(--dur) ease,
    background var(--dur) ease;
}

.box :deep(svg) {
  opacity: 0;
  transition: opacity var(--dur) ease;
}

.check input:checked + .box {
  background: var(--blue-700);
  border-color: var(--blue-700);
}

.check input:checked + .box :deep(svg) {
  opacity: 1;
}

.check input:focus-visible + .box {
  box-shadow: var(--ring);
}

.check input:disabled ~ * {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
