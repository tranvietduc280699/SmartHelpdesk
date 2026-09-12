<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    id: string
    placeholder?: string
    rows?: number
    invalid?: boolean
    disabled?: boolean
    /** Hiện bộ đếm ký tự khi có giới hạn. */
    maxlength?: number
  }>(),
  { rows: 5 },
)

const model = defineModel<string>({ required: true })

const describedBy = computed(() =>
  props.invalid ? `${props.id}-error` : undefined,
)
</script>

<template>
  <div class="input-wrap">
    <textarea
      :id="id"
      v-model="model"
      class="control no-icon"
      :class="{ 'is-error': invalid }"
      :rows="rows"
      :placeholder="placeholder"
      :disabled="disabled"
      :maxlength="maxlength"
      :aria-invalid="invalid || undefined"
      :aria-describedby="describedBy"
    ></textarea>
    <p v-if="maxlength" class="counter">{{ model.length }}/{{ maxlength }}</p>
  </div>
</template>

<style scoped>
.control.no-icon {
  padding-left: 14px;
  line-height: 1.6;
}

.counter {
  margin: 6px 0 0;
  text-align: right;
  font-size: 12px;
  color: var(--slate-400);
  font-variant-numeric: tabular-nums;
}
</style>
