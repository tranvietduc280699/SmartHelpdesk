<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppButton from '@/components/ui/AppButton.vue'
import FormBanner from '@/components/ui/FormBanner.vue'
import { useAuthStore } from '@/stores/auth'
import AppIcon from '@/components/ui/AppIcon.vue'
import CreateRequestModal from '@/features/requests/CreateRequestModal.vue'
import RequestBrowser from '@/features/requests/RequestBrowser.vue'
import RequestStats from '@/features/requests/RequestStats.vue'
import type { SupportRequest } from '@/types/request'

const router = useRouter()
const auth = useAuthStore()

const creating = ref(false)

async function onCreated(request: SupportRequest): Promise<void> {
  creating.value = false
  // Vào thẳng yêu cầu vừa tạo: người dùng muốn thấy nó có thật, và đây cũng
  // là nơi họ theo dõi tiến độ về sau.
  await router.push({ name: 'request-detail', params: { id: request.id } })
}
</script>

<template>
  <div>
    <header class="page-head">
      <div>
        <h1>Yêu cầu của tôi</h1>
        <p>Gửi yêu cầu hỗ trợ mới và theo dõi tiến độ xử lý.</p>
      </div>
      <AppButton @click="creating = true">
        <AppIcon name="plus" :size="17" />
        <span>Tạo yêu cầu</span>
      </AppButton>
    </header>

    <FormBanner v-if="!auth.user?.companyId" tone="warning"
      >Tài khoản chưa có thông tin công ty. Liên hệ quản trị viên để liên kết
      công ty trước khi tạo yêu cầu hỗ trợ.</FormBanner
    >
    <RequestStats />

    <RequestBrowser
      :show-client="false"
      empty-title="Bạn chưa gửi yêu cầu nào"
      empty-description="Gặp lỗi hay cần thêm tính năng? Gửi yêu cầu để đội hỗ trợ tiếp nhận."
    >
      <template #empty-action>
        <AppButton @click="creating = true">
          <AppIcon name="plus" :size="17" />
          <span>Tạo yêu cầu đầu tiên</span>
        </AppButton>
      </template>
    </RequestBrowser>

    <CreateRequestModal
      :open="creating"
      @close="creating = false"
      @created="onCreated"
    />
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.page-head h1 {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--navy-900);
}

.page-head p {
  margin: 0;
  color: var(--slate-600);
  font-size: 14.5px;
}
</style>
