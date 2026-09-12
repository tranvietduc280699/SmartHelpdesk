<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AlertBell from '@/features/alerts/AlertBell.vue'
import ChatInbox from '@/features/chat/ChatInbox.vue'
import { useAlertStore } from '@/stores/alerts'
import { useAuthStore } from '@/stores/auth'
import { ROLE_LABEL } from '@/types/user'

const auth = useAuthStore()
const alerts = useAlertStore()
const route = useRoute()
const router = useRouter()

const currentRequestChat = computed(() => {
  const requestId = route.params.id
  return route.name === 'request-detail' && typeof requestId === 'string'
    ? { name: 'request-detail', params: { id: requestId }, hash: '#chat' }
    : null
})

async function onLogout(): Promise<void> {
  // Chờ backend thu hồi refresh token xong mới rời trang.
  await auth.logout()
  // Thông báo là của riêng từng người — không để sót sang phiên sau.
  alerts.reset()
  await router.push({ name: 'login' })
}
</script>

<template>
  <div class="layout">
    <header class="topbar">
      <div class="topbar__brand">
        <span class="logo__mark">
          <AppIcon name="headset" :size="20" />
        </span>
        <span class="logo__text">KITS</span>
      </div>

      <div v-if="auth.user" class="topbar__user">
        <AlertBell />
        <ChatInbox />
        <RouterLink
          v-if="currentRequestChat"
          class="chat-shortcut"
          :to="currentRequestChat"
          title="Mở chat của request này"
          aria-label="Mở chat của request này"
        >
          <AppIcon name="message-circle" :size="19" />
        </RouterLink>
        <div class="who">
          <span class="who__name">{{ auth.user.name }}</span>
          <span class="who__role">{{ ROLE_LABEL[auth.user.role] }}</span>
        </div>
        <AppButton variant="ghost" @click="onLogout">
          <AppIcon name="log-out" :size="17" />
          <span>Đăng xuất</span>
        </AppButton>
      </div>
    </header>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--slate-50);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 24px;
  background: var(--white);
  border-bottom: 1px solid var(--slate-200);
}

.topbar__brand {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  font-weight: 800;
  font-size: 18px;
  color: var(--navy-900);
}

.logo__mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--blue-700);
  display: grid;
  place-items: center;
  color: var(--white);
  flex: 0 0 auto;
}

.topbar__user {
  display: inline-flex;
  align-items: center;
  gap: 14px;
}

.chat-shortcut {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 8px;
  color: var(--slate-600);
  text-decoration: none;
}

.chat-shortcut:hover {
  background: var(--blue-50);
  color: var(--blue-700);
}

.chat-shortcut:focus-visible {
  outline: none;
  box-shadow: var(--ring);
}

.who {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.25;
}

.who__name {
  font-weight: 600;
  font-size: 14px;
  color: var(--navy-900);
}

.who__role {
  font-size: 12px;
  color: var(--slate-500);
}

.content {
  flex: 1;
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 24px 56px;
}

@media (max-width: 600px) {
  .topbar {
    padding: 12px 16px;
  }

  .who {
    display: none;
  }

  .logo__text {
    display: none;
  }

  .content {
    padding: 24px 16px 40px;
  }
}
</style>
