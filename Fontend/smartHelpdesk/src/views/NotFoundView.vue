<script setup lang="ts">
import { RouterLink } from 'vue-router'
import AppIcon from '@/components/ui/AppIcon.vue'
</script>

<template>
  <div class="wrap">
    <div class="message">
      <p class="code">404</p>
      <h1>Không tìm thấy trang</h1>
      <p class="lede">
        Đường dẫn bạn mở không tồn tại hoặc đã bị đổi. Kiểm tra lại địa chỉ giúp
        chúng tôi nhé.
      </p>
      <RouterLink class="back" to="/">
        <AppIcon name="arrow-left" :size="16" />
        <span>Quay về trang chủ</span>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: var(--slate-50);
}

.message {
  text-align: center;
  max-width: 480px;
}

.code {
  font-size: 64px;
  font-weight: 800;
  color: var(--slate-200);
  margin: 0;
  line-height: 1;
  letter-spacing: -0.03em;
}

h1 {
  font-size: 23px;
  font-weight: 800;
  color: var(--navy-900);
  margin: 12px 0 10px;
  letter-spacing: -0.02em;
}

.lede {
  color: var(--slate-600);
  font-size: 14.5px;
  margin: 0 0 24px;
}

.back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--blue-700);
  font-weight: 600;
  font-size: 14.5px;
  text-decoration: none;
}

.back:hover {
  text-decoration: underline;
}
</style>
