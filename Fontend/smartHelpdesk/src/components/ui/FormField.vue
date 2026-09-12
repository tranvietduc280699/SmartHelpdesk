<script setup lang="ts">
import AppIcon from './AppIcon.vue'

defineProps<{
  label: string
  /** id của control bên trong, để <label for> trỏ đúng. */
  forId: string
  required?: boolean
  /** Câu thông báo lỗi, hoặc undefined khi hợp lệ. */
  error?: string
  hint?: string
}>()
</script>

<template>
  <div class="field">
    <label class="lbl" :for="forId">
      {{ label }}<span v-if="required" class="req"> *</span>
    </label>

    <slot />

    <p v-if="hint" class="hint">{{ hint }}</p>

    <!-- id khớp với aria-describedby mà các control tự dựng từ id của chúng -->
    <p v-if="error" :id="`${forId}-error`" class="err-msg">
      <AppIcon name="alert-circle" :size="14" />
      <span>{{ error }}</span>
    </p>
  </div>
</template>
