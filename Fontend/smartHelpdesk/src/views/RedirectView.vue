<script setup lang="ts">
/**
 * Route '/' chỉ là điểm vào — guard trong router luôn chuyển tiếp sang trang
 * chủ đúng vai trò trước khi component này kịp hiển thị. Nó tồn tại vì
 * vue-router bắt buộc mỗi route phải có component.
 */
</script>

<template>
  <p class="redirecting">Đang chuyển hướng…</p>
</template>

<style scoped>
.redirecting {
  color: var(--slate-500);
  font-size: 14.5px;
  text-align: center;
  padding: 48px 0;
}
</style>
