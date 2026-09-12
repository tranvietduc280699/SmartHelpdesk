<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import type { IconName } from './icons'

const props = withDefaults(
  defineProps<{
    id: string
    icon?: IconName
    type?: 'text' | 'email' | 'tel' | 'url'
    placeholder?: string
    autocomplete?: string
    invalid?: boolean
    required?: boolean
    disabled?: boolean
  }>(),
  { type: 'text' },
)

const model = defineModel<string>({ required: true })

/** Trỏ tới <p class="err-msg"> do FormField render, chỉ khi đang lỗi. */
const describedBy = computed(() =>
  props.invalid ? `${props.id}-error` : undefined,
)
</script>

<template>
  <div class="input-wrap">
    <AppIcon v-if="icon" :name="icon" class="lead" :size="18" />
    <input
      :id="id"
      v-model="model"
      class="control"
      :class="{ 'is-error': invalid, 'no-icon': !icon }"
      :type="type"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      :required="required"
      :disabled="disabled"
      :aria-invalid="invalid || undefined"
      :aria-describedby="describedBy"
    />
  </div>
</template>

<style scoped>
.control.no-icon {
  padding-left: 14px;
}
</style>
