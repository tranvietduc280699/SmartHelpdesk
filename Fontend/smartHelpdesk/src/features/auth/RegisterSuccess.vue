<script setup lang="ts">
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'

const emit = defineEmits<{ backToLogin: [] }>()
</script>

<template>
  <section class="success">
    <div class="success__badge">
      <AppIcon name="check-circle" :size="38" />
    </div>

    <h2>Đăng ký thành công!</h2>
    <p>Tài khoản của bạn đã được tạo và có thể đăng nhập ngay.</p>

    <AppButton block @click="emit('backToLogin')">Đăng nhập ngay</AppButton>
  </section>
</template>

<style scoped>
.success {
  text-align: center;
  padding: 8px 0;
}

.success__badge {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: var(--green-50);
  display: grid;
  place-items: center;
  margin: 0 auto 22px;
  color: var(--green-600);
}

.success h2 {
  font-size: 24px;
  font-weight: 800;
  color: var(--navy-900);
  margin: 0 0 10px;
  letter-spacing: -0.02em;
}

.success p {
  color: var(--slate-600);
  font-size: 14.5px;
  margin: 0 auto 22px;
  max-width: 380px;
}
</style>
