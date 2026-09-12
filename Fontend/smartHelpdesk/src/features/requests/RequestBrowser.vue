<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppPagination from '@/components/ui/AppPagination.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import FormBanner from '@/components/ui/FormBanner.vue'
import TextInput from '@/components/ui/TextInput.vue'
import { CATEGORY_TONE, PRIORITY_TONE, STATUS_TONE } from './badges'
import { ApiError } from '@/services/http'
import { requestService } from '@/services/requestService'
import type { ListParams } from '@/services/requestService'
import type { Page, SupportRequest } from '@/types/request'
import { CATEGORY_LABEL, PRIORITY_LABEL, STATUS_LABEL } from '@/types/request'
import { formatDateTime, fromNow } from '@/utils/datetime'

const props = withDefaults(
  defineProps<{
    /** Cột người tạo / người xử lý — mỗi vai trò cần cột khác nhau. */
    showClient?: boolean
    showDeveloper?: boolean
    emptyTitle?: string
    emptyDescription?: string
  }>(),
  {
    showClient: true,
    showDeveloper: true,
    emptyTitle: 'Chưa có yêu cầu nào',
  },
)

const PAGE_SIZE = 10

const filters = reactive({
  q: '',
  status: '' as NonNullable<ListParams['status']>,
  category: '' as NonNullable<ListParams['category']>,
  priority: '' as NonNullable<ListParams['priority']>,
  sort: 'createdAt,desc',
})

const page = ref(0)
const result = ref<Page<SupportRequest> | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

const rows = computed(() => result.value?.content ?? [])
const hasFilter = computed(
  () =>
    filters.q.trim() !== '' ||
    filters.status !== '' ||
    filters.category !== '' ||
    filters.priority !== '',
)

const STATUS_OPTIONS = [
  { value: '', label: 'Mọi trạng thái' },
  { value: 'PENDING', label: STATUS_LABEL.PENDING },
  { value: 'IN_PROGRESS', label: STATUS_LABEL.IN_PROGRESS },
  { value: 'DONE', label: STATUS_LABEL.DONE },
]

const CATEGORY_OPTIONS = [
  { value: '', label: 'Mọi loại' },
  { value: 'BUG', label: CATEGORY_LABEL.BUG },
  { value: 'FEATURE', label: CATEGORY_LABEL.FEATURE },
  { value: 'INQUIRY', label: CATEGORY_LABEL.INQUIRY },
]

const PRIORITY_OPTIONS = [
  { value: '', label: 'Mọi mức ưu tiên' },
  { value: 'HIGH', label: PRIORITY_LABEL.HIGH },
  { value: 'MEDIUM', label: PRIORITY_LABEL.MEDIUM },
  { value: 'LOW', label: PRIORITY_LABEL.LOW },
]

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Mới nhất' },
  { value: 'createdAt,asc', label: 'Cũ nhất' },
  { value: 'updatedAt,desc', label: 'Vừa cập nhật' },
]

let loadVersion = 0
async function load(): Promise<void> {
  const version = ++loadVersion
  loading.value = true
  error.value = null
  try {
    const data = await requestService.list({
      page: page.value,
      size: PAGE_SIZE,
      sort: filters.sort,
      status: filters.status,
      category: filters.category,
      priority: filters.priority,
      q: filters.q.trim(),
    })
    if (version !== loadVersion) return
    result.value = data
  } catch (cause) {
    if (version !== loadVersion) return
    error.value =
      cause instanceof ApiError
        ? cause.message
        : 'Không tải được danh sách yêu cầu.'
    result.value = null
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

/* Gõ tới đâu gọi API tới đó thì quá tốn; chờ người dùng ngừng gõ đã. */
let debounce: ReturnType<typeof setTimeout> | undefined
watch(
  () => filters.q,
  () => {
    clearTimeout(debounce)
    debounce = setTimeout(() => {
      if (page.value !== 0) page.value = 0
      else void load()
    }, 300)
  },
)
onBeforeUnmount(() => {
  clearTimeout(debounce)
  loadVersion++
})

// Đổi bộ lọc thì quay về trang đầu — đang ở trang 5 mà lọc còn 2 trang
// thì người dùng nhìn thấy bảng rỗng và tưởng là mất dữ liệu.
watch(
  () => [filters.status, filters.category, filters.priority, filters.sort],
  () => {
    if (page.value !== 0) page.value = 0
    else void load()
  },
)

watch(page, () => void load())

function clearFilters(): void {
  filters.q = ''
  filters.status = ''
  filters.category = ''
  filters.priority = ''
}

function setStatusFilter(status: ListParams['status'] = ''): void {
  filters.status = status ?? ''
}

void load()

defineExpose({ reload: load, setStatusFilter })
</script>

<template>
  <section class="browser">
    <div class="toolbar">
      <div class="toolbar__search">
        <TextInput
          id="req-search"
          v-model="filters.q"
          icon="search"
          placeholder="Tìm theo tiêu đề hoặc mô tả…"
        />
      </div>

      <!--
        Mỗi ô lọc cần một nhãn: nội dung ô đổi theo lựa chọn nên trình đọc
        màn hình không đoán được nó lọc cái gì. Nhãn ẩn về mặt hình ảnh
        nhưng vẫn đọc được.
      -->
      <div class="toolbar__filters">
        <div class="filter">
          <label class="sr-only" for="req-status">Lọc theo trạng thái</label>
          <AppSelect
            id="req-status"
            v-model="filters.status"
            :options="STATUS_OPTIONS"
          />
        </div>
        <div class="filter">
          <label class="sr-only" for="req-category">Lọc theo loại</label>
          <AppSelect
            id="req-category"
            v-model="filters.category"
            :options="CATEGORY_OPTIONS"
          />
        </div>
        <div class="filter">
          <label class="sr-only" for="req-priority">Lọc theo mức ưu tiên</label>
          <AppSelect
            id="req-priority"
            v-model="filters.priority"
            :options="PRIORITY_OPTIONS"
          />
        </div>
        <div class="filter">
          <label class="sr-only" for="req-sort">Sắp xếp</label>
          <AppSelect
            id="req-sort"
            v-model="filters.sort"
            :options="SORT_OPTIONS"
          />
        </div>
      </div>
    </div>

    <FormBanner v-if="error"
      >{{ error }}
      <button type="button" class="link" @click="load">
        Thử lại
      </button></FormBanner
    >

    <div class="card">
      <div v-if="loading" class="skeleton" aria-live="polite">
        <span class="sr-only">Đang tải danh sách yêu cầu…</span>
        <div v-for="n in 5" :key="n" class="skeleton__row"></div>
      </div>

      <EmptyState
        v-else-if="!error && rows.length === 0"
        :title="hasFilter ? 'Không có yêu cầu nào khớp bộ lọc' : emptyTitle"
        :description="
          hasFilter
            ? 'Thử bỏ bớt điều kiện lọc hoặc tìm bằng từ khoá khác.'
            : emptyDescription
        "
      >
        <button
          v-if="hasFilter"
          type="button"
          class="link"
          @click="clearFilters"
        >
          Xoá bộ lọc
        </button>
        <slot v-else name="empty-action" />
      </EmptyState>

      <template v-else>
        <div class="table-scroll">
          <table class="table">
            <thead>
              <tr>
                <th scope="col">Yêu cầu</th>
                <th scope="col">Ưu tiên</th>
                <th scope="col">Trạng thái</th>
                <th v-if="props.showClient" scope="col">Người tạo</th>
                <th v-if="props.showDeveloper" scope="col">Người xử lý</th>
                <th scope="col">Cập nhật</th>
                <th v-if="$slots.actions" scope="col">
                  <span class="sr-only">Hành động</span>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="request in rows" :key="request.id">
                <td>
                  <RouterLink
                    class="row-title"
                    :to="{ name: 'request-detail', params: { id: request.id } }"
                  >
                    {{ request.title }}
                  </RouterLink>
                  <RouterLink
                    class="detail-link"
                    :to="{ name: 'request-detail', params: { id: request.id } }"
                  >
                    Xem chi tiết
                  </RouterLink>
                  <AppBadge :tone="CATEGORY_TONE[request.category]">
                    {{ CATEGORY_LABEL[request.category] }}
                  </AppBadge>
                </td>
                <td>
                  <AppBadge :tone="PRIORITY_TONE[request.priority]">
                    {{ PRIORITY_LABEL[request.priority] }}
                  </AppBadge>
                </td>
                <td>
                  <AppBadge dot :tone="STATUS_TONE[request.status]">
                    {{ STATUS_LABEL[request.status] }}
                  </AppBadge>
                </td>
                <td v-if="props.showClient" class="cell-person">
                  {{ request.client.name }}
                </td>
                <td v-if="props.showDeveloper" class="cell-person">
                  <span v-if="request.assignedDeveloper" class="assignee">
                    {{ request.assignedDeveloper.name }}
                  </span>
                  <span v-else class="unassigned">
                    <AppIcon name="alert-triangle" :size="13" />
                    Chưa giao
                  </span>
                </td>
                <td
                  class="cell-time"
                  :title="formatDateTime(request.updatedAt)"
                >
                  {{ fromNow(request.updatedAt) }}
                </td>
                <td v-if="$slots.actions" class="cell-actions">
                  <slot name="actions" :request="request" :reload="load" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <AppPagination
          v-if="result"
          :page="result.page"
          :size="result.size"
          :total-pages="result.totalPages"
          :total-elements="result.totalElements"
          @change="page = $event"
        />
      </template>
    </div>
  </section>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.toolbar__search {
  flex: 1 1 260px;
  min-width: 0;
}

.toolbar__filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

/* Ô lọc thấp hơn ô nhập của form — đây là thanh công cụ, không phải form. */
.toolbar :deep(.control) {
  height: 42px;
  font-size: 14px;
}

.toolbar__filters :deep(.control) {
  min-width: 155px;
}

.card {
  background: var(--white);
  border: 1px solid var(--slate-200);
  border-radius: var(--radius);
  overflow: hidden;
}

.table-scroll {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.table th {
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--slate-500);
  background: var(--slate-50);
  padding: 11px 16px;
  border-bottom: 1px solid var(--slate-200);
  white-space: nowrap;
}

.table td {
  padding: 13px 16px;
  border-bottom: 1px solid var(--slate-100);
  vertical-align: middle;
}

.table tbody tr:last-child td {
  border-bottom: 0;
}

.table tbody tr:hover {
  background: var(--slate-50);
}

.row-title {
  display: block;
  font-weight: 600;
  color: var(--navy-900);
  text-decoration: none;
  margin-bottom: 5px;
  max-width: 420px;
}

.row-title:hover {
  color: var(--blue-700);
  text-decoration: underline;
}

.detail-link {
  display: inline-block;
  margin: 0 0 7px;
  color: var(--blue-700);
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
}

.detail-link:hover {
  text-decoration: underline;
}

.detail-link:focus-visible {
  outline: none;
  box-shadow: var(--ring);
  border-radius: 4px;
}

.row-title:focus-visible {
  outline: none;
  box-shadow: var(--ring);
  border-radius: 4px;
}

.cell-person {
  color: var(--slate-600);
  white-space: nowrap;
}

.unassigned {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--amber-700);
  font-size: 13px;
}

.assignee {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.cell-time {
  color: var(--slate-500);
  white-space: nowrap;
}

.cell-actions {
  text-align: right;
  white-space: nowrap;
}

/* ------------------------------ Đang tải ------------------------------ */
.skeleton {
  padding: 16px;
}

.skeleton__row {
  height: 44px;
  border-radius: 8px;
  background: linear-gradient(
    90deg,
    var(--slate-100) 25%,
    var(--slate-50) 37%,
    var(--slate-100) 63%
  );
  background-size: 400% 100%;
  animation: shimmer 1.3s ease infinite;
}

.skeleton__row + .skeleton__row {
  margin-top: 8px;
}

@keyframes shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
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

@media (max-width: 700px) {
  .toolbar__filters {
    width: 100%;
  }

  .toolbar__filters :deep(.control) {
    min-width: 0;
  }

  .toolbar__filters > .filter {
    flex: 1 1 46%;
  }
}
</style>
