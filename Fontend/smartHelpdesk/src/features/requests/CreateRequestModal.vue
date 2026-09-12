<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import AppTextarea from '@/components/ui/AppTextarea.vue'
import FormBanner from '@/components/ui/FormBanner.vue'
import FormField from '@/components/ui/FormField.vue'
import TextInput from '@/components/ui/TextInput.vue'
import { useFormErrors } from '@/composables/useFormErrors'
import { ApiError } from '@/services/http'
import { requestService } from '@/services/requestService'
import type {
  RequestCategory,
  RequestPriority,
  SupportRequest,
} from '@/types/request'
import { CATEGORY_LABEL, PRIORITY_LABEL } from '@/types/request'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: []; created: [request: SupportRequest] }>()

type Field = 'title' | 'description' | 'category' | 'priority'
const FIELD_ORDER: readonly Field[] = [
  'title',
  'description',
  'category',
  'priority',
]

const { errors, hasErrors, set, merge, clear, reset, firstInvalid } =
  useFormErrors<Field>()

function initialForm() {
  return {
    title: '',
    description: '',
    category: 'BUG' as RequestCategory,
    priority: 'MEDIUM' as RequestPriority,
  }
}

const form = reactive(initialForm())
const loading = ref(false)
const banner = ref<string | null>(null)

/* ---------------------------- Gợi ý từ AI ---------------------------- */
const suggesting = ref(false)
const suggestion = ref<string | null>(null)

/**
 * Gọi endpoint LLM của đề bài để đoán loại và mức ưu tiên từ phần mô tả.
 * Chỉ điền sẵn vào ô, không tự gửi: người dùng vẫn là người quyết định.
 */
async function askAi(): Promise<void> {
  if (suggesting.value || loading.value) return
  if (!form.description.trim()) {
    set('description', 'Vui lòng nhập mô tả để AI phân tích.')
    return
  }

  suggesting.value = true
  suggestion.value = null
  try {
    const description = form.description.trim()
    const result = await requestService.suggest(description)
    if (!props.open || form.description.trim() !== description) return
    if (result.category) form.category = result.category
    if (result.priority) form.priority = result.priority
    const parts: string[] = []
    if (result.category) parts.push(`loại ${CATEGORY_LABEL[result.category]}`)
    if (result.priority)
      parts.push(`mức ưu tiên ${PRIORITY_LABEL[result.priority]}`)
    suggestion.value = `AI đề xuất ${parts.join(', ')}. ${result.reason}`
  } catch (cause) {
    suggestion.value =
      cause instanceof ApiError
        ? `Không lấy được gợi ý: ${cause.message}`
        : 'Không lấy được gợi ý từ AI.'
  } finally {
    suggesting.value = false
  }
}

/* ------------------------------ Gửi đi ------------------------------ */
const CATEGORY_OPTIONS = (Object.keys(CATEGORY_LABEL) as RequestCategory[]).map(
  (value) => ({ value, label: CATEGORY_LABEL[value] }),
)

const PRIORITY_OPTIONS = (Object.keys(PRIORITY_LABEL) as RequestPriority[]).map(
  (value) => ({ value, label: PRIORITY_LABEL[value] }),
)

function validate(): boolean {
  reset()
  if (!form.title.trim()) set('title', 'Vui lòng nhập tiêu đề.')
  if (form.title.trim().length > 255) set('title', 'Tiêu đề tối đa 255 ký tự.')
  return !hasErrors.value
}

function focusFirstInvalid(): void {
  const field = firstInvalid(FIELD_ORDER)
  if (field) document.getElementById(`new-${field}`)?.focus()
}

async function onSubmit(): Promise<void> {
  if (loading.value || suggesting.value) return
  banner.value = null
  if (!validate()) {
    focusFirstInvalid()
    return
  }

  loading.value = true
  try {
    const created = await requestService.create({
      title: form.title.trim(),
      description: form.description.trim(),
      category: form.category,
      priority: form.priority,
    })
    emit('created', created)
  } catch (cause) {
    if (cause instanceof ApiError) {
      banner.value = cause.message
      merge(cause.fieldErrors as Partial<Record<Field, string>>)
    } else {
      banner.value = 'Không gửi được yêu cầu. Vui lòng thử lại.'
    }
  } finally {
    loading.value = false
  }
}

// Mở lại là form sạch: giữ nội dung lần trước dễ khiến người dùng gửi nhầm
// một yêu cầu cũ mà không để ý.
watch(
  () => props.open,
  (open) => {
    if (!open) return
    Object.assign(form, initialForm())
    reset()
    banner.value = null
    suggestion.value = null
  },
)
</script>

<template>
  <AppModal
    :open="open"
    title="Tạo yêu cầu hỗ trợ"
    :busy="loading || suggesting"
    :width="620"
    @close="emit('close')"
  >
    <FormBanner v-if="banner">{{ banner }}</FormBanner>

    <form id="create-request-form" novalidate @submit.prevent="onSubmit">
      <FormField
        label="Tiêu đề"
        for-id="new-title"
        required
        :error="errors.title"
        hint="Một câu ngắn mô tả vấn đề, ví dụ: Không xuất được báo cáo tháng 7."
      >
        <TextInput
          id="new-title"
          v-model="form.title"
          placeholder="Tóm tắt ngắn gọn vấn đề"
          :invalid="!!errors.title"
          :disabled="loading"
          @update:model-value="clear('title')"
        />
      </FormField>

      <FormField
        label="Mô tả chi tiết"
        for-id="new-description"
        :error="errors.description"
        hint="Càng cụ thể càng xử lý nhanh: thao tác đang làm, kết quả mong đợi, kết quả thực tế."
      >
        <AppTextarea
          id="new-description"
          v-model="form.description"
          :rows="6"
          placeholder="Mô tả tình huống, các bước tái hiện, ảnh hưởng tới ai…"
          :invalid="!!errors.description"
          :disabled="loading"
          @update:model-value="clear('description')"
        />
      </FormField>

      <div class="ai">
        <AppButton
          variant="ghost"
          :loading="suggesting"
          :disabled="loading"
          @click="askAi"
        >
          <AppIcon name="sparkles" :size="16" />
          <span>Nhờ AI phân loại giúp</span>
        </AppButton>
        <p v-if="suggestion" class="ai__note">{{ suggestion }}</p>
      </div>

      <div class="grid-2">
        <FormField
          label="Loại yêu cầu"
          for-id="new-category"
          required
          :error="errors.category"
        >
          <AppSelect
            id="new-category"
            v-model="form.category"
            :options="CATEGORY_OPTIONS"
            :disabled="loading"
          />
        </FormField>

        <FormField
          :error="errors.priority"
          label="Mức ưu tiên"
          for-id="new-priority"
          required
          hint="Chọn Cao khi công việc đang bị dừng hẳn."
        >
          <AppSelect
            id="new-priority"
            v-model="form.priority"
            :options="PRIORITY_OPTIONS"
            :disabled="loading"
          />
        </FormField>
      </div>
    </form>

    <template #footer>
      <AppButton variant="ghost" :disabled="loading" @click="emit('close')">
        Huỷ
      </AppButton>
      <AppButton
        type="submit"
        form="create-request-form"
        :loading="loading"
        :disabled="suggesting"
      >
        Gửi yêu cầu
      </AppButton>
    </template>
  </AppModal>
</template>

<style scoped>
.ai {
  margin: 0 0 16px;
}

.ai :deep(.btn) {
  height: 40px;
  font-size: 14px;
}

.ai__note {
  margin: 10px 0 0;
  padding: 10px 12px;
  background: var(--blue-50);
  border: 1px solid var(--blue-100);
  border-radius: var(--radius-sm);
  color: var(--navy-800);
  font-size: 13px;
  line-height: 1.55;
}
</style>
