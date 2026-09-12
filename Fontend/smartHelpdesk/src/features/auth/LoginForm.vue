<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppButton from '@/components/ui/AppButton.vue'
import AppCheckbox from '@/components/ui/AppCheckbox.vue'
import FormBanner from '@/components/ui/FormBanner.vue'
import FormField from '@/components/ui/FormField.vue'
import PasswordInput from '@/components/ui/PasswordInput.vue'
import TextInput from '@/components/ui/TextInput.vue'
import { useFormErrors } from '@/composables/useFormErrors'
import { ApiError } from '@/services/http'
import { ROLE_HOME } from '@/router/roleHome'
import { useAuthStore } from '@/stores/auth'
import { isEmail } from '@/utils/validators'

const emit = defineEmits<{ switchToRegister: [] }>()

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

type Field = 'email' | 'password'
const { errors, hasErrors, set, merge, clear, reset } = useFormErrors<Field>()

const form = reactive({ email: '', password: '', remember: true })
const loading = ref(false)
const banner = ref<string | null>(null)

function validate(): boolean {
  reset()
  if (!isEmail(form.email)) set('email', 'Vui lòng nhập email hợp lệ.')
  if (!form.password) set('password', 'Vui lòng nhập mật khẩu.')
  return !hasErrors.value
}

async function onSubmit(): Promise<void> {
  if (loading.value) return
  banner.value = null
  if (!validate()) return

  loading.value = true
  try {
    const loggedInUser = await auth.login(
      { email: form.email.trim(), password: form.password },
      form.remember,
    )
    // Quay lại đúng trang người dùng định vào trước khi bị guard chặn.
    const redirect = route.query.redirect
    await router.replace(
      typeof redirect === 'string' &&
        redirect.startsWith('/') &&
        !redirect.startsWith('//') &&
        !redirect.includes('\\')
        ? redirect
        : ROLE_HOME[loggedInUser.role],
    )
  } catch (error) {
    if (error instanceof ApiError) {
      banner.value = error.message
      merge(error.fieldErrors as Partial<Record<Field, string>>)
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
      <h2>Chào mừng trở lại</h2>
      <p>Đăng nhập để tiếp tục với hệ thống quản lý yêu cầu.</p>
    </div>

    <FormBanner v-if="banner">{{ banner }}</FormBanner>

    <form novalidate @submit.prevent="onSubmit">
      <FormField
        label="Email"
        for-id="login-email"
        required
        :error="errors.email"
      >
        <TextInput
          id="login-email"
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

      <FormField
        label="Mật khẩu"
        for-id="login-password"
        required
        :error="errors.password"
      >
        <PasswordInput
          id="login-password"
          v-model="form.password"
          placeholder="Nhập mật khẩu"
          autocomplete="current-password"
          :invalid="!!errors.password"
          :disabled="loading"
          @update:model-value="clear('password')"
        />
      </FormField>

      <div class="row-between">
        <AppCheckbox v-model="form.remember" :disabled="loading">
          Ghi nhớ đăng nhập
        </AppCheckbox>
        <span class="hint">Quên mật khẩu? Liên hệ quản trị viên.</span>
      </div>

      <AppButton type="submit" block :loading="loading">Đăng nhập</AppButton>
    </form>

    <p class="switch-line">
      Chưa có tài khoản?
      <button type="button" class="link" @click="emit('switchToRegister')">
        Đăng ký ngay
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

.row-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 4px 0 22px;
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
