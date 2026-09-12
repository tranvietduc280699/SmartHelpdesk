<script setup lang="ts">
import { computed, ref } from 'vue'
import AppButton from '@/components/ui/AppButton.vue'
import { ApiError } from '@/services/http'
import { requestService } from '@/services/requestService'
import type { RequestStatus, SupportRequest } from '@/types/request'
import { nextStatuses, STATUS_LABEL } from '@/types/request'

const props = defineProps<{ request: SupportRequest }>()
const emit = defineEmits<{
  updated: [request: SupportRequest]
  failed: [message: string]
}>()

const memo = ref('')
const busy = ref<RequestStatus | null>(null)

/**
 * Chỉ hiện nước đi hợp lệ. Luật thật nằm ở backend — chỗ này chỉ để người
 * dùng không bấm vào thứ chắc chắn sẽ bị từ chối.
 */
const options = computed(() => nextStatuses(props.request.status))

/** Nhãn theo hành động, dễ hiểu hơn là tên trạng thái đích. */
const ACTION_LABEL: Record<RequestStatus, string> = {
  PENDING: STATUS_LABEL.PENDING,
  IN_PROGRESS: 'Bắt đầu xử lý',
  DONE: 'Đánh dấu hoàn thành',
}

async function change(status: RequestStatus): Promise<void> {
  if (busy.value) return
  busy.value = status
  try {
    emit(
      'updated',
      await requestService.updateStatus(props.request.id, status, memo.value),
    )
    memo.value = ''
  } catch (cause) {
    emit(
      'failed',
      cause instanceof ApiError
        ? cause.message
        : 'Không đổi được trạng thái yêu cầu.',
    )
  } finally {
    busy.value = null
  }
}
</script>

<template>
  <span v-if="options.length" class="actions">
    <input
      v-model="memo"
      class="memo"
      aria-label="Ghi chú cập nhật trạng thái"
      placeholder="Ghi chú (không bắt buộc)"
      :disabled="busy !== null"
    />
    <AppButton
      v-for="status in options"
      :key="status"
      variant="ghost"
      :loading="busy === status"
      :disabled="busy !== null"
      @click="change(status)"
    >
      {{ ACTION_LABEL[status] }}
    </AppButton>
  </span>
</template>

<style scoped>
.actions {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
}

.actions :deep(.btn) {
  height: 36px;
  font-size: 13.5px;
  padding: 0 12px;
}
.memo {
  border: 1px solid var(--slate-200);
  border-radius: 6px;
  padding: 8px;
  max-width: 220px;
  font: inherit;
}
</style>
