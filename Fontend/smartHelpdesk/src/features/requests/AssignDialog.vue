<script setup lang="ts">
import { ref, watch } from 'vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppModal from '@/components/ui/AppModal.vue'
import FormBanner from '@/components/ui/FormBanner.vue'
import { ApiError } from '@/services/http'
import { memberService } from '@/services/memberService'
import { requestService } from '@/services/requestService'
import type { SupportRequest } from '@/types/request'
import type { Member } from '@/types/user'

const props = defineProps<{
  open: boolean
  request: SupportRequest | null
}>()

const emit = defineEmits<{ close: []; assigned: [request: SupportRequest] }>()

const developers = ref<Member[]>([])
/** Rỗng nghĩa là để backend tự chọn theo luật auto-assign. */
const picked = ref('')
const loading = ref(false)
const error = ref<string | null>(null)

watch(
  () => props.open,
  async (open) => {
    if (!open) return
    developers.value = []
    picked.value = props.request?.assignedDeveloper?.id ?? ''
    error.value = null
    try {
      developers.value = (await memberService.list('dev')).filter(
        (member) => member.status?.toLowerCase() === 'active',
      )
    } catch {
      error.value = 'Không tải được danh sách developer.'
    }
  },
)

async function submit(): Promise<void> {
  if (!props.request || loading.value) return
  loading.value = true
  error.value = null
  try {
    const updated = await requestService.assign(
      props.request.id,
      picked.value || undefined,
    )
    emit('assigned', updated)
  } catch (cause) {
    error.value =
      cause instanceof ApiError ? cause.message : 'Phân công không thành công.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AppModal
    :open="open"
    title="Phân công developer"
    :busy="loading"
    :width="520"
    @close="emit('close')"
  >
    <FormBanner v-if="error">{{ error }}</FormBanner>

    <p v-if="request" class="target">
      Yêu cầu: <strong>{{ request.title }}</strong>
    </p>

    <fieldset class="choices">
      <legend class="sr-only">Chọn người xử lý</legend>

      <label class="choice choice--auto">
        <input v-model="picked" type="radio" name="assignee" value="" />
        <span class="choice__body">
          <span class="choice__title">
            <AppIcon name="sparkles" :size="15" />
            Để hệ thống tự chọn
          </span>
          <span class="choice__desc">
            Giao cho developer đang ít việc chưa xong nhất; nếu bằng nhau thì
            chọn người vừa hoàn thành gần đây nhất.
          </span>
        </span>
      </label>

      <label v-for="dev in developers" :key="dev.id" class="choice">
        <input v-model="picked" type="radio" name="assignee" :value="dev.id" />
        <span class="choice__body">
          <span class="choice__title">{{ dev.name }}</span>
          <span class="choice__desc">{{ dev.email }}</span>
        </span>
      </label>
    </fieldset>

    <template #footer>
      <AppButton variant="ghost" :disabled="loading" @click="emit('close')">
        Huỷ
      </AppButton>
      <AppButton :loading="loading" @click="submit">Phân công</AppButton>
    </template>
  </AppModal>
</template>

<style scoped>
.target {
  margin: 0 0 16px;
  font-size: 14px;
  color: var(--slate-600);
}

.target strong {
  color: var(--navy-900);
}

.choices {
  border: 0;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.choice {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  padding: 12px 14px;
  border: 1.5px solid var(--slate-200);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition:
    border-color var(--dur) ease,
    background var(--dur) ease;
}

.choice:hover {
  border-color: var(--blue-300);
}

.choice:has(input:checked) {
  border-color: var(--blue-700);
  background: var(--blue-50);
}

.choice:has(input:focus-visible) {
  box-shadow: var(--ring);
}

.choice input {
  margin-top: 3px;
  accent-color: var(--blue-700);
  flex: 0 0 auto;
}

.choice__body {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.choice__title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14.5px;
  color: var(--navy-900);
}

.choice__desc {
  font-size: 12.5px;
  color: var(--slate-500);
  line-height: 1.5;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
