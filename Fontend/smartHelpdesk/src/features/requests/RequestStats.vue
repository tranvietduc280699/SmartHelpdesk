<script setup lang="ts">
import { ref } from 'vue'
import StatCard from '@/components/ui/StatCard.vue'
import { useAuthStore } from '@/stores/auth'
import FormBanner from '@/components/ui/FormBanner.vue'
import { requestService } from '@/services/requestService'
import type { RequestStats } from '@/types/request'
import { STATUS_LABEL } from '@/types/request'

withDefaults(defineProps<{ showRate?: boolean }>(), { showRate: false })

const stats = ref<RequestStats | null>(null)
const failed = ref(false)

async function load(): Promise<void> {
  try {
    if (useAuthStore().hasRole('admin'))
      stats.value = await requestService.stats()
    else {
      const [pending, progress, done] = await Promise.all([
        requestService.list({ status: 'PENDING', size: 1 }),
        requestService.list({ status: 'IN_PROGRESS', size: 1 }),
        requestService.list({ status: 'DONE', size: 1 }),
      ])
      const byStatus = {
        PENDING: pending.totalElements,
        IN_PROGRESS: progress.totalElements,
        DONE: done.totalElements,
      }
      const total = byStatus.PENDING + byStatus.IN_PROGRESS + byStatus.DONE
      stats.value = {
        total,
        completed: byStatus.DONE,
        byStatus,
        completionRate: total ? byStatus.DONE / total : 0,
        byDeveloper: [],
      }
    }
    failed.value = false
  } catch {
    // Allow retry without blocking the request list.
    failed.value = true
  }
}

void load()

defineExpose({ reload: load })
</script>

<template>
  <FormBanner v-if="failed"
    >Không tải được thống kê.
    <button type="button" class="link" @click="load">
      Thử lại
    </button></FormBanner
  >
  <template v-if="stats && !failed">
    <div class="stats">
      <StatCard
        label="Tổng yêu cầu"
        :value="stats.total"
        icon="file-check"
        tone="neutral"
      />
      <StatCard
        v-if="stats.byStatus"
        :label="STATUS_LABEL.PENDING"
        :value="stats.byStatus.PENDING"
        icon="clock"
        tone="warning"
      />
      <StatCard
        v-if="stats.byStatus"
        :label="STATUS_LABEL.IN_PROGRESS"
        :value="stats.byStatus.IN_PROGRESS"
        icon="construction"
        tone="info"
      />
      <StatCard
        :label="STATUS_LABEL.DONE"
        :value="stats.completed"
        icon="check-circle"
        tone="success"
      />
      <StatCard
        v-if="showRate"
        label="Tỉ lệ hoàn thành"
        :value="`${Math.round(stats.completionRate * 100)}%`"
        icon="bar-chart"
        tone="success"
        :hint="`${stats.completed}/${stats.total} yêu cầu`"
      />
    </div>

    <!-- Chỗ cho Admin gắn thêm bảng theo developer, nằm ngoài lưới ô số. -->
    <slot name="extra" :stats="stats" />
  </template>
</template>

<style scoped>
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 22px;
}

@media (max-width: 520px) {
  .stats {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
