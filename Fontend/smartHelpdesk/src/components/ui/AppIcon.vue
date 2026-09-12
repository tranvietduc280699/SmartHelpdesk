<script setup lang="ts">
import { computed } from 'vue'
import { ICONS } from './icons'
import type { IconName } from './icons'

const props = withDefaults(
  defineProps<{
    name: IconName
    /** Cỡ mặc định tính bằng px. CSS ở component cha vẫn ghi đè được. */
    size?: number
    strokeWidth?: number
    /** Đặt false khi icon mang nghĩa và cần được trình đọc màn hình đọc. */
    decorative?: boolean
  }>(),
  { size: 20, strokeWidth: 2, decorative: true },
)

const markup = computed(() => ICONS[props.name])
</script>

<template>
  <!--
    v-html an toàn ở đây: nội dung lấy từ hằng ICONS trong mã nguồn,
    không bao giờ đến từ người dùng hay từ API.
  -->
  <!-- eslint-disable vue/no-v-html -->
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    :aria-hidden="decorative ? 'true' : undefined"
    focusable="false"
    v-html="markup"
  />
</template>
