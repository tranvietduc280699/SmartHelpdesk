<script setup lang="ts">
import { useRouter } from 'vue-router'
import AppButton from '@/components/ui/AppButton.vue'
import { useAuthStore } from '@/stores/auth'
import { ROLE_HOME } from '@/router/roleHome'

const auth = useAuthStore()
const router = useRouter()

function goHome(): void {
  void router.push(auth.role ? ROLE_HOME[auth.role] : { name: 'login' })
}
</script>

<template>
  <div class="message">
    <p class="code">403</p>
    <h1>Bạn không có quyền vào trang này</h1>
    <p class="lede">
      Trang bạn vừa mở dành cho vai trò khác. Nếu cho rằng đây là nhầm lẫn, hãy
      liên hệ Admin để được cấp quyền.
    </p>
    <AppButton @click="goHome">Về trang chủ của tôi</AppButton>
  </div>
</template>

<style scoped>
.message {
  text-align: center;
  padding: 48px 24px;
  max-width: 520px;
  margin: 0 auto;
}

.code {
  font-size: 56px;
  font-weight: 800;
  color: var(--slate-200);
  margin: 0;
  line-height: 1;
  letter-spacing: -0.03em;
}

h1 {
  font-size: 22px;
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
</style>
