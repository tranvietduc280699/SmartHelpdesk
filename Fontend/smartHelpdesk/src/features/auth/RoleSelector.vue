<script setup lang="ts">
import AppIcon from '@/components/ui/AppIcon.vue'
import type { IconName } from '@/components/ui/icons'
import type { SelectableRole } from '@/types/user'

/** Public registration excludes ADMIN. */
interface RoleOption {
  value: SelectableRole
  icon: IconName
  title: string
  desc: string
}

const OPTIONS: RoleOption[] = [
  {
    value: 'client',
    icon: 'user',
    title: 'Client',
    desc: 'Gửi & theo dõi yêu cầu',
  },
  {
    value: 'dev',
    icon: 'code',
    title: 'Developer',
    desc: 'Nhận & xử lý yêu cầu',
  },
]

defineProps<{ disabled?: boolean }>()
const model = defineModel<SelectableRole>({ required: true })
</script>

<template>
  <!--
    fieldset + legend thay cho <label> đơn lẻ ở bản trước: một nhãn không gắn
    với control nào là sai chuẩn, trình đọc màn hình không biết nó mô tả gì.
  -->
  <fieldset class="role-fieldset" :disabled="disabled">
    <legend class="lbl">Vai trò <span class="req">*</span></legend>

    <div class="role-grid">
      <label v-for="option in OPTIONS" :key="option.value" class="role-card">
        <input v-model="model" type="radio" name="role" :value="option.value" />
        <span class="role-card__ico">
          <AppIcon :name="option.icon" :size="20" />
        </span>
        <span class="role-card__title">{{ option.title }}</span>
        <span class="role-card__desc">{{ option.desc }}</span>
        <span class="role-card__check" aria-hidden="true">
          <AppIcon name="check" :size="12" :stroke-width="3" />
        </span>
      </label>
    </div>

    <p class="hint">
      Tài khoản Admin được cấp phát riêng bởi quản trị hệ thống.
    </p>
  </fieldset>
</template>

<style scoped>
.role-fieldset {
  border: 0;
  padding: 0;
  margin: 0 0 16px;
  min-width: 0;
}

.role-fieldset legend {
  padding: 0;
}

.role-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.role-card {
  position: relative;
  border: 1.5px solid var(--slate-200);
  border-radius: var(--radius-sm);
  padding: 14px;
  cursor: pointer;
  background: var(--white);
  transition:
    border-color var(--dur) ease,
    background var(--dur) ease;
}

.role-card:hover {
  border-color: var(--blue-300);
}

.role-card input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.role-card__ico {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: var(--slate-100);
  display: grid;
  place-items: center;
  margin-bottom: 10px;
  color: var(--slate-600);
  transition:
    background var(--dur) ease,
    color var(--dur) ease;
}

.role-card__title {
  display: block;
  font-weight: 700;
  font-size: 14.5px;
  color: var(--navy-900);
}

.role-card__desc {
  display: block;
  font-size: 12px;
  color: var(--slate-500);
  margin-top: 3px;
  line-height: 1.35;
}

.role-card__check {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid var(--slate-200);
  display: grid;
  place-items: center;
  color: var(--white);
  transition:
    border-color var(--dur) ease,
    background var(--dur) ease;
}

.role-card__check :deep(svg) {
  opacity: 0;
  transition: opacity var(--dur) ease;
}

.role-card input:checked ~ .role-card__check {
  background: var(--blue-700);
  border-color: var(--blue-700);
}

.role-card input:checked ~ .role-card__check :deep(svg) {
  opacity: 1;
}

.role-card:has(input:checked) {
  border-color: var(--blue-700);
  background: var(--blue-50);
}

.role-card:has(input:checked) .role-card__ico {
  background: var(--blue-100);
  color: var(--blue-700);
}

.role-card input:focus-visible ~ .role-card__check {
  box-shadow: var(--ring);
}

@media (max-width: 480px) {
  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
