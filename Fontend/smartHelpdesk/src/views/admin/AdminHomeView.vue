<script setup lang="ts">
import { ref, useTemplateRef } from 'vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import FormBanner from '@/components/ui/FormBanner.vue'
import AssignDialog from '@/features/requests/AssignDialog.vue'
import RequestBrowser from '@/features/requests/RequestBrowser.vue'
import RequestStats from '@/features/requests/RequestStats.vue'
import { CATEGORY_LABEL } from '@/types/request'
import type { SupportRequest } from '@/types/request'

const banner = ref<string | null>(null)
const assigning = ref<SupportRequest | null>(null)
const stats = useTemplateRef<InstanceType<typeof RequestStats>>('stats')
const browser = useTemplateRef<InstanceType<typeof RequestBrowser>>('browser')

function refreshAll(): void {
  browser.value?.reload()
  stats.value?.reload()
}

function onAssigned(): void {
  assigning.value = null
  banner.value = null
  refreshAll()
}
</script>

<template>
  <div>
    <header class="page-head">
      <h1>Quản trị</h1>
      <p>Toàn bộ yêu cầu trong hệ thống, phân công và theo dõi tiến độ.</p>
    </header>

    <FormBanner v-if="banner">{{ banner }}</FormBanner>

    <RequestStats ref="stats" show-rate>
      <template #extra="{ stats: data }">
        <section v-if="data.byCategory" class="workload">
          <h2>Yêu cầu theo danh mục</h2>
          <ul>
            <li v-for="(count, category) in data.byCategory" :key="category">
              <span>{{ CATEGORY_LABEL[category] }}</span
              ><strong>{{ count }}</strong>
            </li>
          </ul>
        </section>
        <section class="workload">
          <h2>
            <AppIcon name="users" :size="17" />
            Khối lượng theo developer
          </h2>
          <p v-if="!data.byDeveloper.length">Chưa có dữ liệu phân công.</p>
          <ul>
            <li v-for="row in data.byDeveloper" :key="row.developer.id">
              <span class="workload__name">{{ row.developer.name }}</span>
              <span class="workload__bar" aria-hidden="true">
                <span
                  class="workload__fill"
                  :style="{
                    width: `${row.assigned === 0 ? 0 : Math.round((row.done / row.assigned) * 100)}%`,
                  }"
                ></span>
              </span>
              <span class="workload__num">
                {{ row.done }}/{{ row.assigned }} xong · {{ row.pending }} chờ ·
                {{ row.inProgress }} đang xử lý
              </span>
            </li>
          </ul>
        </section>
      </template>
    </RequestStats>

    <RequestBrowser ref="browser">
      <template #actions="{ request }">
        <AppButton
          v-if="request.status !== 'DONE'"
          variant="ghost"
          @click="assigning = request"
        >
          <AppIcon name="user-check" :size="15" />
          <span>{{
            request.assignedDeveloper ? 'Đổi người' : 'Phân công'
          }}</span>
        </AppButton>
      </template>
    </RequestBrowser>

    <AssignDialog
      :open="assigning !== null"
      :request="assigning"
      @close="assigning = null"
      @assigned="onAssigned"
    />
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

.workload {
  background: var(--white);
  border: 1px solid var(--slate-200);
  border-radius: var(--radius);
  padding: 16px 18px 18px;
  margin-bottom: 22px;
}

.workload h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 700;
  color: var(--navy-900);
}

.workload ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.workload li {
  display: grid;
  grid-template-columns: 150px 1fr auto;
  align-items: center;
  gap: 14px;
  font-size: 13.5px;
}

.workload__name {
  color: var(--navy-800);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workload__bar {
  height: 8px;
  border-radius: 999px;
  background: var(--slate-100);
  overflow: hidden;
}

.workload__fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--blue-700);
  transition: width var(--dur) ease;
}

.workload__num {
  color: var(--slate-500);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 600px) {
  .workload li {
    grid-template-columns: 1fr auto;
  }

  .workload__bar {
    grid-column: 1 / -1;
    order: 3;
  }
}

:deep(.cell-actions .btn) {
  height: 36px;
  font-size: 13.5px;
  padding: 0 12px;
}
</style>
