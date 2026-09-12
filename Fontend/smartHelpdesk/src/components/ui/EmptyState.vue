<script setup lang="ts">
import AppIcon from './AppIcon.vue'
import type { IconName } from './icons'

withDefaults(
  defineProps<{
    icon?: IconName
    title: string
    description?: string
  }>(),
  { icon: 'inbox' },
)
</script>

<template>
  <div class="empty">
    <span class="empty__ico">
      <AppIcon :name="icon" :size="26" />
    </span>
    <p class="empty__title">{{ title }}</p>
    <p v-if="description" class="empty__desc">{{ description }}</p>
    <!-- Chỗ đặt nút hành động, ví dụ "Tạo yêu cầu đầu tiên". -->
    <slot />
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 56px 24px;
}

.empty__ico {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--slate-100);
  color: var(--slate-400);
  display: grid;
  place-items: center;
  margin-bottom: 16px;
}

.empty__title {
  margin: 0;
  font-weight: 700;
  font-size: 15.5px;
  color: var(--navy-900);
}

.empty__desc {
  margin: 6px 0 0;
  font-size: 14px;
  color: var(--slate-500);
  max-width: 380px;
}

.empty :slotted(*) {
  margin-top: 20px;
}
</style>
