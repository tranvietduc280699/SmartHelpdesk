<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import FormBanner from '@/components/ui/FormBanner.vue'
import FormField from '@/components/ui/FormField.vue'
import PasswordInput from '@/components/ui/PasswordInput.vue'
import PasswordStrength from '@/components/ui/PasswordStrength.vue'
import TextInput from '@/components/ui/TextInput.vue'
import RoleSelector from './RoleSelector.vue'
import { useFormErrors } from '@/composables/useFormErrors'
import {
  companyService,
  DEVELOPER_COMPANY_ID,
  isClientCompany,
  type Company,
} from '@/services/companyService'
import { ApiError } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import type { SelectableRole } from '@/types/user'
import { isEmail, passwordScore } from '@/utils/validators'

const emit = defineEmits<{ registered: []; switchToLogin: [] }>()

const auth = useAuthStore()

const companies = ref<Company[]>([])
const companiesLoading = ref(false)
const companiesError = ref<string | null>(null)
const companyOptions = computed(() =>
  companies.value.map((company) => ({
    value: company.companyId,
    label: company.companyName,
  })),
)
async function loadCompanies(): Promise<void> {
  if (companiesLoading.value) return
  companiesLoading.value = true
  companiesError.value = null
  try {
    companies.value = (await companyService.list()).filter((company) =>
      isClientCompany(company.companyId),
    )
    if (
      !companies.value.some((company) => company.companyId === form.companyId)
    ) {
      form.companyId = ''
    }
  } catch {
    form.companyId = ''
    companies.value = []
    companiesError.value =
      'Không tải được danh sách công ty. Vui lòng thử lại hoặc liên hệ quản trị viên.'
  } finally {
    companiesLoading.value = false
  }
}
onMounted(loadCompanies)
type Field = 'name' | 'email' | 'phone' | 'company' | 'password' | 'confirm'

const FIELD_ORDER: readonly Field[] = [
  'name',
  'email',
  'phone',
  'company',
  'password',
  'confirm',
]

const { errors, hasErrors, set, merge, clear, reset, firstInvalid } =
  useFormErrors<Field>()

function initialForm() {
  return {
    name: '',
    email: '',
    phone: '',
    role: 'client' as SelectableRole,
    companyId: '',
    password: '',
    confirm: '',
  }
}

const form = reactive(initialForm())
const loading = ref(false)
const banner = ref<string | null>(null)

const score = computed(() => passwordScore(form.password))

// Đổi sang dev thì bỏ công ty đã chọn — cột company_id không áp dụng.
watch(
  () => form.role,
  (role) => {
    if (role !== 'client') {
      form.companyId = ''
      clear('company')
    }
  },
)

/* ------------------------------ Kiểm tra ------------------------------- */
function validate(): boolean {
  reset()

  if (!form.name.trim()) set('name', 'Vui lòng nhập họ và tên.')
  if (!isEmail(form.email)) set('email', 'Email không hợp lệ.')
  if (form.name.trim().length > 100) set('name', 'Họ tên tối đa 100 ký tự.')
  if (form.email.trim().length > 100) set('email', 'Email tối đa 100 ký tự.')
  if (form.phone.trim().length > 20)
    set('phone', 'Số điện thoại tối đa 20 ký tự.')
  if (
    form.role === 'client' &&
    !companyOptions.value.some((company) => company.value === form.companyId)
  )
    set('company', 'Vui lòng chọn công ty.')
  if (!form.password.trim()) set('password', 'Vui lòng nhập mật khẩu.')
  if (!form.confirm) set('confirm', 'Vui lòng xác nhận mật khẩu.')
  else if (form.confirm !== form.password)
    set('confirm', 'Mật khẩu xác nhận không khớp.')

  return !hasErrors.value
}

function focusFirstInvalid(): void {
  const field = firstInvalid(FIELD_ORDER)
  if (!field) return
  const el = document.getElementById(`reg-${field}`)
  el?.focus({ preventScroll: false })
}

async function onSubmit(): Promise<void> {
  if (loading.value) return
  banner.value = null

  if (!validate()) {
    banner.value = 'Vui lòng kiểm tra lại các trường được đánh dấu.'
    focusFirstInvalid()
    return
  }

  loading.value = true
  try {
    await auth.register({
      name: form.name.trim(),
      email: form.email.trim(),
      phone: form.phone.trim() || null,
      role: form.role,
      companyId: form.role === 'client' ? form.companyId : DEVELOPER_COMPANY_ID,
      password: form.password,
    })
    Object.assign(form, initialForm())
    emit('registered')
  } catch (error) {
    if (error instanceof ApiError) {
      banner.value = error.message
      merge(error.fieldErrors as Partial<Record<Field, string>>)
      if (error.fieldErrors.companyId)
        set('company', error.fieldErrors.companyId)
    } else {
      banner.value = 'Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section>
    <div class="form-head">
      <h2>Tạo tài khoản mới</h2>
      <p>Đăng ký để gửi và theo dõi yêu cầu hỗ trợ.</p>
    </div>

    <FormBanner v-if="banner">{{ banner }}</FormBanner>

    <form novalidate @submit.prevent="onSubmit">
      <FormField
        label="Họ và tên"
        for-id="reg-name"
        required
        :error="errors.name"
      >
        <TextInput
          id="reg-name"
          v-model="form.name"
          icon="user"
          placeholder="Nhập họ và tên"
          autocomplete="name"
          :invalid="!!errors.name"
          :disabled="loading"
          @update:model-value="clear('name')"
        />
      </FormField>

      <FormField
        label="Email"
        for-id="reg-email"
        required
        :error="errors.email"
      >
        <TextInput
          id="reg-email"
          v-model="form.email"
          type="email"
          icon="mail"
          placeholder="ban@congty.com"
          autocomplete="email"
          :invalid="!!errors.email"
          :disabled="loading"
          @update:model-value="clear('email')"
        />
      </FormField>

      <FormField label="Số điện thoại" for-id="reg-phone" :error="errors.phone">
        <TextInput
          id="reg-phone"
          v-model="form.phone"
          type="tel"
          icon="phone"
          placeholder="09xxxxxxxx"
          autocomplete="tel"
          :invalid="!!errors.phone"
          :disabled="loading"
          @update:model-value="clear('phone')"
        />
      </FormField>

      <RoleSelector v-model="form.role" :disabled="loading" />

      <FormField
        v-if="form.role === 'client'"
        label="Công ty"
        for-id="reg-company"
        required
        :error="errors.company"
      >
        <AppSelect
          id="reg-company"
          v-model="form.companyId"
          :options="companyOptions"
          required
          icon="building"
          :placeholder="
            companiesLoading ? 'Đang tải công ty…' : 'Chọn công ty của bạn'
          "
          :invalid="!!errors.company"
          :disabled="
            loading || companiesLoading || !!companiesError || !companies.length
          "
          @update:model-value="clear('company')"
        />
      </FormField>

      <p v-if="form.role === 'dev'" class="hint">Công ty: BZCOM</p>
      <FormBanner v-if="form.role === 'client' && companiesError">
        {{ companiesError }}
        <button
          type="button"
          class="link"
          :disabled="companiesLoading || loading"
          @click="loadCompanies"
        >
          Thử lại
        </button>
      </FormBanner>
      <p
        v-else-if="
          form.role === 'client' && !companiesLoading && !companies.length
        "
        role="status"
      >
        Chưa có công ty để lựa chọn. Vui lòng liên hệ quản trị viên.
      </p>
      <div class="grid-2">
        <FormField
          label="Mật khẩu"
          for-id="reg-password"
          required
          :error="errors.password"
        >
          <PasswordInput
            id="reg-password"
            v-model="form.password"
            placeholder="Nhập mật khẩu"
            autocomplete="new-password"
            :invalid="!!errors.password"
            :disabled="loading"
            @update:model-value="clear('password')"
          />
          <PasswordStrength :score="score" />
        </FormField>

        <FormField
          label="Xác nhận mật khẩu"
          for-id="reg-confirm"
          required
          :error="errors.confirm"
        >
          <PasswordInput
            id="reg-confirm"
            v-model="form.confirm"
            placeholder="Nhập lại mật khẩu"
            autocomplete="new-password"
            :invalid="!!errors.confirm"
            :disabled="loading"
            @update:model-value="clear('confirm')"
          />
        </FormField>
      </div>

      <AppButton
        type="submit"
        block
        :loading="loading"
        :disabled="
          form.role === 'client' &&
          (companiesLoading || !!companiesError || !companies.length)
        "
        >Tạo tài khoản</AppButton
      >
    </form>

    <p class="switch-line">
      Đã có tài khoản?
      <button type="button" class="link" @click="emit('switchToLogin')">
        Đăng nhập
      </button>
    </p>
  </section>
</template>

<style scoped>
.form-head {
  margin-bottom: 24px;
}

.form-head h2 {
  font-size: 25px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--navy-900);
  margin: 0 0 6px;
}

.form-head p {
  margin: 0;
  color: var(--slate-600);
  font-size: 14.5px;
}

.switch-line {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--slate-600);
}

@media (max-width: 480px) {
  .form-head h2 {
    font-size: 22px;
  }
}
</style>
