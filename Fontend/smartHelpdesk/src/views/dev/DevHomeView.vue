<script setup lang="ts">
import { computed, ref, useTemplateRef } from 'vue'
import FormBanner from '@/components/ui/FormBanner.vue'
import RequestBrowser from '@/features/requests/RequestBrowser.vue'
import RequestStats from '@/features/requests/RequestStats.vue'
import StatusActions from '@/features/requests/StatusActions.vue'
import type { ListParams } from '@/services/requestService'

const banner = ref<string | null>(null)
const stats = useTemplateRef<InstanceType<typeof RequestStats>>('stats')
const browser = useTemplateRef<InstanceType<typeof RequestBrowser>>('browser')
const selectedStatus = ref<ListParams['status']>('')

const STATUS_VIEWS = [
  { value: '', label: 'Tất cả' },
  { value: 'PENDING', label: 'Chờ xử lý' },
  { value: 'IN_PROGRESS', label: 'Đang xử lý' },
  { value: 'DONE', label: 'Hoàn thành' },
] as const

const selectedStatusLabel = computed(
  () => STATUS_VIEWS.find((view) => view.value === selectedStatus.value)?.label,
)

function selectStatus(status: ListParams['status']): void {
  selectedStatus.value = status
  browser.value?.setStatusFilter(status)
}

/** Đổi trạng thái xong thì cả bảng lẫn ô số liệu đều phải cập nhật theo. */
function afterChange(reload: () => void): void {
  banner.value = null
  reload()
  stats.value?.reload()
}
</script>

<template>
  <div>
    <header class="page-head">
      <h1>Việc được giao</h1>
      <p>Các yêu cầu được phân công cho bạn. Lọc và sắp xếp để theo dõi tiến độ.</p>
    </header>

    <FormBanner v-if="banner">{{ banner }}</FormBanner>

    <RequestStats ref="stats" />

    <nav class="status-views" aria-label="Xem request theo trạng thái">
      <span class="status-views__label">Request của tôi:</span>
      <button
        v-for="view in STATUS_VIEWS"
        :key="view.value || 'all'"
        type="button"
        class="status-view"
        :class="{ 'status-view--active': selectedStatus === view.value }"
        :aria-pressed="selectedStatus === view.value"
        @click="selectStatus(view.value)"
      >
        {{ view.label }}
      </button>
      <span class="sr-only">{{ selectedStatusLabel }}</span>
    </nav>

    <RequestBrowser
      ref="browser"
      :show-developer="false"
      empty-title="Chưa có việc nào được giao"
      empty-description="Khi Admin phân công một yêu cầu cho bạn, nó sẽ xuất hiện ở đây."
    >
      <template #actions="{ request, reload }">
        <StatusActions
          :request="request"
          @updated="afterChange(reload)"
          @failed="banner = $event"
        />
      </template>
    </RequestBrowser>
  </div>
</template>

<style scoped>
.page-head {
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

.status-views {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0 0 18px;
}

.status-views__label {
  margin-right: 4px;
  color: var(--slate-600);
  font-size: 13px;
  font-weight: 600;
}

.status-view {
  border: 1px solid var(--slate-200);
  border-radius: 6px;
  padding: 8px 12px;
  background: var(--white);
  color: var(--slate-600);
  cursor: pointer;
  font: inherit;
  font-size: 13px;
}

.status-view:hover,
.status-view--active {
  border-color: var(--blue-700);
  background: var(--blue-700);
  color: var(--white);
}
</style>
