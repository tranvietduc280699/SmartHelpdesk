<script setup lang="ts">
import { computed, ref } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps<{
  id: string
  placeholder?: string
  /** 'current-password' khi đăng nhập, 'new-password' khi đăng ký/đổi. */
  autocomplete?: string
  invalid?: boolean
  required?: boolean
  disabled?: boolean
}>()

const model = defineModel<string>({ required: true })

const visible = ref(false)

const describedBy = computed(() =>
  props.invalid ? `${props.id}-error` : undefined,
)
</script>

<template>
  <div class="input-wrap">
    <AppIcon name="lock" class="lead" :size="18" />
    <input
      :id="id"
      v-model="model"
      class="control pw"
      :class="{ 'is-error': invalid }"
      :type="visible ? 'text' : 'password'"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      :required="required"
      :disabled="disabled"
      :aria-invalid="invalid || undefined"
      :aria-describedby="describedBy"
    />
    <button
      type="button"
      class="toggle-pw"
      :aria-label="visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
      :aria-pressed="visible"
      :disabled="disabled"
      @click="visible = !visible"
    >
      <AppIcon :name="visible ? 'eye-off' : 'eye'" :size="19" />
    </button>
  </div>
</template>

<style scoped>
.toggle-pw {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 34px;
  height: 34px;
  border: 0;
  background: transparent;
  display: grid;
  place-items: center;
  border-radius: 8px;
  cursor: pointer;
  color: var(--slate-500);
  transition:
    color var(--dur) ease,
    background var(--dur) ease;
}

.toggle-pw:hover:not(:disabled) {
  color: var(--navy-900);
  background: var(--slate-100);
}

.toggle-pw:focus-visible {
  outline: none;
  box-shadow: var(--ring);
}

.toggle-pw:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
