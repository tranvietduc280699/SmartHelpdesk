<script setup lang="ts">
import { computed, nextTick, ref, useTemplateRef } from 'vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AuthBrandPanel from '@/features/auth/AuthBrandPanel.vue'
import LoginForm from '@/features/auth/LoginForm.vue'
import RegisterForm from '@/features/auth/RegisterForm.vue'
import RegisterSuccess from '@/features/auth/RegisterSuccess.vue'

const TABS = [
  { id: 'login', label: 'Đăng nhập' },
  { id: 'register', label: 'Đăng ký' },
] as const

type TabId = (typeof TABS)[number]['id']
type View = TabId | 'success'

const view = ref<View>('login')
const tabButtons = useTemplateRef<HTMLButtonElement[]>('tabButtons')

/** Màn hình "đăng ký thành công" không thuộc bộ tab nào. */
const showTabs = computed(() => view.value !== 'success')

async function selectTab(id: TabId, moveFocus = false): Promise<void> {
  view.value = id
  if (!moveFocus) return
  await nextTick()
  const index = TABS.findIndex((tab) => tab.id === id)
  tabButtons.value?.[index]?.focus()
}

/**
 * Điều hướng tab bằng bàn phím theo chuẩn WAI-ARIA (mũi tên / Home / End).
 * Bản trước thiếu phần này nên người dùng bàn phím bị kẹt ở tab đầu tiên.
 */
function onTabKeydown(event: KeyboardEvent, index: number): void {
  const last = TABS.length - 1
  let target: number | null = null

  if (event.key === 'ArrowRight') target = index === last ? 0 : index + 1
  else if (event.key === 'ArrowLeft') target = index === 0 ? last : index - 1
  else if (event.key === 'Home') target = 0
  else if (event.key === 'End') target = last

  if (target === null) return
  event.preventDefault()
  void selectTab(TABS[target]!.id, true)
}
</script>

<template>
  <div class="shell">
    <AuthBrandPanel />

    <main class="panel">
      <div class="card">
        <div class="mobile-logo">
          <span class="logo__mark">
            <AppIcon name="headset" :size="22" />
          </span>
          <span>KITS</span>
        </div>

        <div
          v-if="showTabs"
          class="tabs"
          role="tablist"
          aria-label="Chọn đăng nhập hoặc đăng ký"
        >
          <button
            v-for="(tab, index) in TABS"
            :id="`tab-${tab.id}`"
            ref="tabButtons"
            :key="tab.id"
            class="tab"
            type="button"
            role="tab"
            :aria-selected="view === tab.id"
            :aria-controls="`panel-${tab.id}`"
            :tabindex="view === tab.id ? 0 : -1"
            @click="selectTab(tab.id)"
            @keydown="onTabKeydown($event, index)"
          >
            {{ tab.label }}
          </button>
        </div>

        <!--
          v-if thay cho display:none ở bản trước: form đang ẩn không còn nằm
          trong DOM nên trình duyệt không autofill nhầm và trình đọc màn hình
          không đọc phải nội dung không hiển thị.
        -->
        <div
          v-if="view === 'login'"
          id="panel-login"
          class="view"
          role="tabpanel"
          aria-labelledby="tab-login"
        >
          <LoginForm @switch-to-register="selectTab('register', true)" />
        </div>

        <div
          v-else-if="view === 'register'"
          id="panel-register"
          class="view"
          role="tabpanel"
          aria-labelledby="tab-register"
        >
          <RegisterForm
            @registered="view = 'success'"
            @switch-to-login="selectTab('login', true)"
          />
        </div>

        <div v-else class="view">
          <RegisterSuccess @back-to-login="selectTab('login', true)" />
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.05fr 1fr;
}

.panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background: var(--slate-50);
}

.card {
  width: 100%;
  max-width: 460px;
}

.mobile-logo {
  display: none;
}

.logo__mark {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  background: var(--blue-700);
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  color: var(--white);
}

/* ------------------------------- Tabs -------------------------------- */
.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: var(--slate-100);
  border: 1px solid var(--slate-200);
  border-radius: var(--radius);
  padding: 5px;
  margin-bottom: 28px;
}

.tab {
  appearance: none;
  border: 0;
  background: transparent;
  font: inherit;
  font-weight: 600;
  font-size: 14.5px;
  color: var(--slate-600);
  padding: 11px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition:
    color var(--dur) ease,
    background var(--dur) ease;
}

.tab[aria-selected='true'] {
  background: var(--white);
  color: var(--navy-900);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.tab:hover:not([aria-selected='true']) {
  color: var(--navy-900);
}

.tab:focus-visible {
  outline: none;
  box-shadow: var(--ring);
}

/* ------------------------------- Views -------------------------------- */
.view {
  animation: fade 0.25s ease;
}

@keyframes fade {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* ----------------------------- Responsive ----------------------------- */
@media (max-width: 960px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .panel {
    padding: 32px 20px 48px;
    align-items: flex-start;
  }

  .mobile-logo {
    display: flex;
    align-items: center;
    gap: 11px;
    justify-content: center;
    margin: 8px 0 30px;
    font-weight: 800;
    font-size: 19px;
    color: var(--navy-900);
  }
}
</style>
