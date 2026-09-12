import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ALL_ROLES, ROLE_HOME } from './roleHome'
import type { UserRole } from '@/types/user'

declare module 'vue-router' {
  interface RouteMeta {
    /** Chỉ dành cho khách chưa đăng nhập (trang login). */
    guestOnly?: boolean
    /** Vai trò được phép vào. Có mặt = route này bắt buộc đăng nhập. */
    roles?: UserRole[]
    /** Ghép vào <title> của tab. */
    title?: string
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/AuthView.vue'),
    meta: { guestOnly: true, title: 'Đăng nhập & Đăng ký' },
  },

  // Vùng đã đăng nhập — dùng chung thanh điều hướng của AppLayout.
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/RedirectView.vue'),
        meta: { roles: ALL_ROLES },
      },
      {
        path: 'client',
        name: 'client-home',
        component: () => import('@/views/client/ClientHomeView.vue'),
        meta: { roles: ['client'], title: 'Yêu cầu của tôi' },
      },
      {
        path: 'dev',
        name: 'dev-home',
        component: () => import('@/views/dev/DevHomeView.vue'),
        meta: { roles: ['dev'], title: 'Việc được giao' },
      },
      {
        path: 'admin',
        name: 'admin-home',
        component: () => import('@/views/admin/AdminHomeView.vue'),
        meta: { roles: ['admin'], title: 'Quản trị' },
      },
      {
        // Cả ba vai trò dùng chung màn chi tiết; backend đã cắt dữ liệu theo
        // vai trò nên ai mở nhầm yêu cầu không thuộc mình sẽ nhận 404.
        path: 'requests/:id',
        name: 'request-detail',
        component: () => import('@/views/RequestDetailView.vue'),
        meta: { roles: ALL_ROLES, title: 'Chi tiết yêu cầu' },
      },
      {
        path: '403',
        name: 'forbidden',
        component: () => import('@/views/ForbiddenView.vue'),
        meta: { roles: ALL_ROLES, title: 'Không có quyền truy cập' },
      },
    ],
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: 'Không tìm thấy trang' },
  },
]

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: (_to, _from, saved) => saved ?? { top: 0 },
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  // Có token nhưng chưa có hồ sơ (vừa F5) — đọc lại phiên từ storage.
  if (auth.hasToken && !auth.isAuthenticated) {
    auth.restore()
  }

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return ROLE_HOME[auth.role!]
  }

  if (to.meta.roles) {
    if (!auth.isAuthenticated) {
      // Nhớ đích đến để đăng nhập xong quay lại đúng chỗ.
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    if (!auth.hasRole(...to.meta.roles)) {
      return { name: 'forbidden' }
    }
  }

  // '/' chỉ là điểm vào — chuyển tiếp tới trang chủ đúng vai trò.
  if (to.name === 'home' && auth.role) {
    return ROLE_HOME[auth.role]
  }

  return true
})

const BASE_TITLE = 'KITS — Smart Helpdesk'
router.afterEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} · ${BASE_TITLE}`
    : BASE_TITLE
})
