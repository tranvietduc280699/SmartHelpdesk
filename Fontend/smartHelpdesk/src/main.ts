import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { useAuthStore } from '@/stores/auth'
import { useAlertStore } from '@/stores/alerts'
import { router } from './router'
import './assets/styles/main.css'

const app = createApp(App)

// Pinia phải cài trước router: guard trong router gọi useAuthStore().
app.use(createPinia())
app.use(router)
window.addEventListener('auth:refreshed', () => useAuthStore().restore())
window.addEventListener('auth:expired', () => {
  useAuthStore().restore()
  useAlertStore().reset()
  if (router.currentRoute.value.name !== 'login')
    void router.replace({
      name: 'login',
      query: { redirect: router.currentRoute.value.fullPath },
    })
})

app.mount('#app')
