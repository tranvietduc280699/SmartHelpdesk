<script setup lang="ts">
import AppIcon from './AppIcon.vue'
import type { IconName } from './icons'

const props = withDefaults(
  defineProps<{
    tone?: 'danger' | 'warning' | 'info'
    icon?: IconName
  }>(),
  { tone: 'danger' },
)

/**
 * role="alert" chỉ có tác dụng khi phần tử được *thêm mới* vào DOM.
 * Vì vậy component này luôn dùng kèm v-if ở phía cha, không phải ẩn bằng CSS
 * như bản trước — nếu không trình đọc màn hình sẽ không đọc thông báo.
 */
const ICON_FALLBACK: Record<'danger' | 'warning' | 'info', IconName> = {
  danger: 'alert-circle',
  warning: 'alert-circle',
  info: 'alert-circle',
}

const iconName = props.icon ?? ICON_FALLBACK[props.tone]
</script>

<template>
  <div class="form-banner" :class="`form-banner--${tone}`" role="alert">
    <AppIcon :name="iconName" :size="18" />
    <span><slot /></span>
  </div>
</template>

<style scoped>
.form-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: var(--radius-sm);
  padding: 11px 14px;
  font-size: 13.5px;
  font-weight: 500;
  margin-bottom: 18px;
}

.form-banner :deep(svg) {
  flex: 0 0 auto;
}

.form-banner--danger {
  background: var(--red-50);
  border: 1px solid var(--red-200);
  color: var(--red-700);
}

.form-banner--warning {
  background: var(--amber-50);
  border: 1px solid var(--amber-200);
  color: var(--amber-800);
}

.form-banner--info {
  background: var(--blue-50);
  border: 1px solid var(--blue-100);
  color: var(--blue-700);
}
</style>
