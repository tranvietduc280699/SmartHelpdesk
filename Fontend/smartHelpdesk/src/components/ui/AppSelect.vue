<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import type { IconName } from './icons'

export interface SelectOption {
  value: string
  label: string
}

const props = defineProps<{
  id: string
  options: readonly SelectOption[]
  icon?: IconName
  /** Dòng hiển thị khi chưa chọn gì. */
  placeholder?: string
  invalid?: boolean
  required?: boolean
  disabled?: boolean
}>()

const model = defineModel<string>({ required: true })

const describedBy = computed(() =>
  props.invalid ? `${props.id}-error` : undefined,
)
</script>

<template>
  <div class="input-wrap">
    <AppIcon v-if="icon" :name="icon" class="lead" :size="18" />
    <select
      :id="id"
      v-model="model"
      class="control"
      :class="{ 'is-error': invalid, 'no-icon': !icon }"
      :required="required"
      :disabled="disabled"
      :aria-invalid="invalid || undefined"
      :aria-describedby="describedBy"
    >
      <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
      <option
        v-for="option in options"
        :key="option.value"
        :value="option.value"
      >
        {{ option.label }}
      </option>
    </select>
  </div>
</template>

<style scoped>
.control.no-icon {
  padding-left: 14px;
}
</style>
