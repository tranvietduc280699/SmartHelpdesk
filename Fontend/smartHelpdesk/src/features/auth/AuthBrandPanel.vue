<script setup lang="ts">
import AppIcon from '@/components/ui/AppIcon.vue'
import type { IconName } from '@/components/ui/icons'

interface Feature {
  icon: IconName
  title: string
  desc: string
}

const FEATURES: Feature[] = [
  {
    icon: 'file-check',
    title: 'Tạo & theo dõi request',
    desc: 'Phân loại BUG / FEATURE / INQUIRY, theo dõi trạng thái từ PENDING đến DONE.',
  },
  {
    icon: 'users',
    title: 'Phân quyền theo vai trò',
    desc: 'Client, Developer và Admin — mỗi vai trò một luồng công việc riêng.',
  },
  {
    icon: 'bell',
    title: 'Thông báo & lịch sử',
    desc: 'Nhận cảnh báo khi được phân công hoặc trạng thái thay đổi.',
  },
]

const YEAR = new Date().getFullYear()
</script>

<template>
  <aside class="brand">
    <div class="brand__inner">
      <div class="logo">
        <span class="logo__mark">
          <AppIcon name="headset" :size="24" />
        </span>
        <span>KITS</span>
      </div>
    </div>

    <div class="brand__headline">
      <span class="brand__eyebrow">Hệ thống quản lý yêu cầu</span>
      <h1>Tiếp nhận, phân công và xử lý yêu cầu — tất cả trong một nơi.</h1>
      <p>
        Kết nối Client, Developer và Admin trong một quy trình xử lý request
        minh bạch, có lịch sử và thông báo thời gian thực.
      </p>
    </div>

    <ul class="features">
      <li v-for="feature in FEATURES" :key="feature.title">
        <span class="ico">
          <AppIcon :name="feature.icon" :size="20" />
        </span>
        <div>
          <div class="ft-title">{{ feature.title }}</div>
          <div class="ft-desc">{{ feature.desc }}</div>
        </div>
      </li>
    </ul>

    <div class="brand__foot">
      <span>© {{ YEAR }} KITS</span>
      <a href="#" @click.prevent>Điều khoản</a>
      <a href="#" @click.prevent>Bảo mật</a>
      <a href="#" @click.prevent>Hỗ trợ</a>
    </div>
  </aside>
</template>

<style scoped>
.brand {
  position: relative;
  overflow: hidden;
  background: var(--navy-900);
  color: var(--slate-200);
  padding: 48px 56px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

/* Trang trí hình học phẳng — không gradient, không đổ bóng */
.brand::before,
.brand::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: var(--navy-decor);
  pointer-events: none;
}

.brand::before {
  width: 460px;
  height: 460px;
  right: -160px;
  top: -160px;
}

.brand::after {
  width: 320px;
  height: 320px;
  left: -120px;
  bottom: -120px;
}

.brand__inner,
.brand__headline,
.features,
.brand__foot {
  position: relative;
  z-index: 1;
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-weight: 800;
  font-size: 20px;
  letter-spacing: -0.01em;
  color: var(--white);
}

.logo__mark {
  width: 42px;
  height: 42px;
  border-radius: 11px;
  background: var(--blue-700);
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  color: var(--white);
}

.brand__headline {
  margin-top: 64px;
  max-width: 460px;
}

.brand__eyebrow {
  display: inline-block;
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--sky-300);
  background: rgba(3, 105, 161, 0.18);
  padding: 6px 12px;
  border-radius: 999px;
  margin-bottom: 22px;
}

.brand__headline h1 {
  font-size: 34px;
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--white);
  margin: 0 0 16px;
}

.brand__headline p {
  font-size: 16px;
  color: var(--slate-400);
  margin: 0;
}

.features {
  list-style: none;
  padding: 0;
  margin: 40px 0 0;
  display: grid;
  gap: 18px;
  max-width: 440px;
}

.features li {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.features .ico {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: var(--navy-decor);
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  color: var(--sky-300);
}

.features .ft-title {
  font-weight: 600;
  color: var(--slate-200);
  font-size: 15px;
}

.features .ft-desc {
  color: var(--slate-500);
  font-size: 13.5px;
  margin-top: 2px;
}

.brand__foot {
  color: var(--slate-500);
  font-size: 13px;
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
}

.brand__foot a {
  color: var(--slate-400);
  text-decoration: none;
}

.brand__foot a:hover {
  color: var(--slate-300);
}

/* Trên màn hình hẹp chỉ hiển thị cột form; logo rút gọn nằm trong AuthView. */
@media (max-width: 960px) {
  .brand {
    display: none;
  }
}
</style>
