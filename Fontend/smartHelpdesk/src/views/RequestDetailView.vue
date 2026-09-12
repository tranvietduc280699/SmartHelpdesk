<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import FormBanner from '@/components/ui/FormBanner.vue'
import AssignDialog from '@/features/requests/AssignDialog.vue'
import ChatPanel from '@/features/chat/ChatPanel.vue'
import StatusActions from '@/features/requests/StatusActions.vue'
import {
  CATEGORY_TONE,
  PRIORITY_TONE,
  STATUS_TONE,
} from '@/features/requests/badges'
import { ApiError } from '@/services/http'
import { requestService } from '@/services/requestService'
import { useAuthStore } from '@/stores/auth'
import { ROLE_HOME } from '@/router/roleHome'
import type { RequestHistory, SupportRequest } from '@/types/request'
import {
  ACTION_LABEL,
  CATEGORY_LABEL,
  PRIORITY_LABEL,
  STATUS_LABEL,
} from '@/types/request'
import { formatDateTime, fromNow } from '@/utils/datetime'

const route = useRoute()
const auth = useAuthStore()

const request = ref<SupportRequest | null>(null)
const history = ref<RequestHistory[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const banner = ref<string | null>(null)
const assigning = ref(false)
const summary = ref<string | null>(null)
const summarizing = ref(false)
async function summarize(): Promise<void> {
  if (!request.value || summarizing.value) return
  const id = request.value.id
  summarizing.value = true
  try {
    const result = await requestService.summary(id)
    if (request.value?.id === id) summary.value = result.summary
  } catch (cause) {
    if (request.value?.id === id)
      banner.value =
        cause instanceof ApiError ? cause.message : 'Không tải được tóm tắt.'
  } finally {
    summarizing.value = false
  }
}

const backTo = computed(() => (auth.role ? ROLE_HOME[auth.role] : '/'))
const canAssign = computed(() => auth.hasRole('admin'))
const canChangeStatus = computed(
  () =>
    auth.hasRole('admin') ||
    (auth.hasRole('dev') &&
      request.value?.assignedDeveloper?.id === auth.user?.id),
)

let loadVersion = 0
async function load(id: string): Promise<void> {
  const version = ++loadVersion
  loading.value = true
  error.value = null
  summary.value = null
  banner.value = null
  assigning.value = false
  try {
    // Hai lời gọi độc lập nhau, chạy song song cho đỡ phải chờ hai lượt.
    const [detail, entries] = await Promise.all([
      requestService.get(id),
      requestService.history(id),
    ])
    if (version !== loadVersion) return
    request.value = detail
    history.value = entries
  } catch (cause) {
    if (version !== loadVersion) return
    error.value =
      cause instanceof ApiError ? cause.message : 'Không tải được yêu cầu này.'
    request.value = null
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

/** Đổi id trên URL (ví dụ bấm thông báo khác) thì nạp lại. */
watch(
  () => route.params.id,
  (id) => {
    request.value = null
    if (typeof id === 'string') void load(id)
  },
  { immediate: true },
)

async function refresh(): Promise<void> {
  banner.value = null
  if (typeof route.params.id === 'string') await load(route.params.id)
}

async function onAssigned(): Promise<void> {
  assigning.value = false
  await refresh()
}
</script>

<template>
  <div>
    <RouterLink class="back" :to="backTo">
      <AppIcon name="arrow-left" :size="16" />
      <span>Quay lại danh sách</span>
    </RouterLink>

    <p v-if="loading" class="state">Đang tải yêu cầu…</p>

    <FormBanner v-else-if="error"
      >{{ error }}
      <button type="button" class="link" @click="refresh">
        Thử lại
      </button></FormBanner
    >

    <template v-else-if="request">
      <FormBanner v-if="banner">{{ banner }}</FormBanner>

      <header class="head">
        <div class="head__main">
          <div class="head__badges">
            <AppBadge dot :tone="STATUS_TONE[request.status]">
              {{ STATUS_LABEL[request.status] }}
            </AppBadge>
            <AppBadge :tone="PRIORITY_TONE[request.priority]">
              Ưu tiên {{ PRIORITY_LABEL[request.priority] }}
            </AppBadge>
            <AppBadge :tone="CATEGORY_TONE[request.category]">
              {{ CATEGORY_LABEL[request.category] }}
            </AppBadge>
          </div>
          <h1>{{ request.title }}</h1>
          <p class="head__meta">
            Tạo bởi <strong>{{ request.client.name }}</strong>
            <span :title="formatDateTime(request.createdAt)">
              · {{ fromNow(request.createdAt) }}
            </span>
          </p>
        </div>

        <div v-if="canAssign || canChangeStatus" class="head__actions">
          <StatusActions
            v-if="canChangeStatus"
            :request="request"
            @updated="refresh"
            @failed="banner = $event"
          />
          <AppButton v-if="canAssign" variant="ghost" @click="assigning = true">
            <AppIcon name="user-check" :size="16" />
            <span>{{
              request.assignedDeveloper ? 'Đổi người' : 'Phân công'
            }}</span>
          </AppButton>
        </div>
      </header>

      <div class="layout">
        <section class="card">
          <h2>Mô tả</h2>
          <p class="description">
            {{ request.description || 'Chưa có mô tả.' }}
          </p>
          <AppButton variant="ghost" :loading="summarizing" @click="summarize"
            >Tóm tắt bằng AI</AppButton
          >
          <p v-if="summary" class="description">{{ summary }}</p>
        </section>

        <aside class="card">
          <h2>Thông tin</h2>
          <dl class="facts">
            <dt>Người xử lý</dt>
            <dd>
              <span v-if="request.assignedDeveloper" class="assignee">
                {{ request.assignedDeveloper.name }}
              </span>
              <span v-else class="muted">Chưa phân công</span>
            </dd>

            <dt>Công ty</dt>
            <dd>{{ request.companyName || request.companyId || '—' }}</dd>
            <dt>Địa chỉ</dt>
            <dd>{{ request.companyAddress || '—' }}</dd>
            <dt>Điện thoại</dt>
            <dd>{{ request.companyPhone || '—' }}</dd>
            <dt>Người tạo</dt>
            <dd>{{ request.client.name }}</dd>

            <dt>Ngày tạo</dt>
            <dd>{{ formatDateTime(request.createdAt) }}</dd>

            <dt>Cập nhật</dt>
            <dd>{{ formatDateTime(request.updatedAt) }}</dd>
          </dl>
        </aside>

        <section class="card card--wide">
          <h2>Lịch sử xử lý</h2>
          <p v-if="!history.length">Chưa có lịch sử xử lý.</p>
          <ol class="timeline">
            <li v-for="entry in history" :key="entry.id">
              <span class="timeline__dot" aria-hidden="true"></span>
              <div class="timeline__body">
                <p class="timeline__title">
                  {{ ACTION_LABEL[entry.action] }}
                  <span
                    v-if="
                      entry.fromStatus &&
                      entry.toStatus &&
                      entry.fromStatus !== entry.toStatus
                    "
                    class="timeline__move"
                  >
                    {{ STATUS_LABEL[entry.fromStatus] }} →
                    {{ STATUS_LABEL[entry.toStatus] }}
                  </span>
                </p>
                <p class="timeline__meta">
                  {{ entry.changedBy.name }} ·
                  <span :title="formatDateTime(entry.changedAt)">
                    {{ fromNow(entry.changedAt) }}
                  </span>
                </p>
                <p v-if="entry.memo" class="timeline__memo">{{ entry.memo }}</p>
              </div>
            </li>
          </ol>
        </section>

        <ChatPanel :request-id="request.id" />
      </div>

      <AssignDialog
        :open="assigning"
        :request="request"
        @close="assigning = false"
        @assigned="onAssigned"
      />
    </template>
  </div>
</template>

<style scoped>
.back {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--slate-600);
  font-size: 13.5px;
  font-weight: 600;
  text-decoration: none;
  margin-bottom: 18px;
}

.back:hover {
  color: var(--blue-700);
}

.back:focus-visible {
  outline: none;
  box-shadow: var(--ring);
  border-radius: 4px;
}

.state {
  color: var(--slate-500);
  font-size: 14.5px;
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
  margin-bottom: 22px;
}

.head__badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.head__main h1 {
  margin: 0 0 6px;
  font-size: 23px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--navy-900);
  max-width: 640px;
}

.head__meta {
  margin: 0;
  font-size: 13.5px;
  color: var(--slate-500);
}

.head__meta strong {
  color: var(--navy-800);
  font-weight: 600;
}

.head__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.head__actions :deep(.btn) {
  height: 40px;
  font-size: 14px;
}

.layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 16px;
  align-items: start;
}

.card {
  background: var(--white);
  border: 1px solid var(--slate-200);
  border-radius: var(--radius);
  padding: 18px 20px 20px;
}

.card--wide {
  grid-column: 1 / -1;
}

.card h2 {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 700;
  color: var(--navy-900);
}

.description {
  margin: 0;
  font-size: 14.5px;
  line-height: 1.7;
  color: var(--navy-800);
  white-space: pre-wrap;
}

.facts {
  margin: 0;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 9px 14px;
  font-size: 13.5px;
}

.facts dt {
  color: var(--slate-500);
}

.facts dd {
  margin: 0;
  color: var(--navy-800);
  font-weight: 600;
  text-align: right;
}

.muted {
  color: var(--amber-700);
  font-weight: 500;
}

.assignee {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

/* ------------------------------ Timeline ------------------------------ */
.timeline {
  list-style: none;
  margin: 0;
  padding: 0;
}

.timeline li {
  position: relative;
  padding: 0 0 20px 26px;
}

.timeline li::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 16px;
  bottom: 0;
  width: 2px;
  background: var(--slate-200);
}

.timeline li:last-child {
  padding-bottom: 0;
}

.timeline li:last-child::before {
  display: none;
}

.timeline__dot {
  position: absolute;
  left: 0;
  top: 5px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--white);
  border: 2.5px solid var(--blue-700);
}

.timeline__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--navy-900);
}

.timeline__move {
  font-weight: 500;
  color: var(--slate-500);
  font-size: 13px;
}

.timeline__meta {
  margin: 3px 0 0;
  font-size: 12.5px;
  color: var(--slate-500);
}

.timeline__memo {
  margin: 7px 0 0;
  padding: 9px 12px;
  background: var(--slate-50);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--slate-600);
  line-height: 1.55;
}

@media (max-width: 860px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
