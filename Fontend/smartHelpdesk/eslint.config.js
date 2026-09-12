import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{ts,mts,tsx,vue,js}'],
  },
  {
    name: 'app/files-to-ignore',
    ignores: ['dist/**', 'coverage/**', 'node_modules/**'],
  },

  js.configs.recommended,
  pluginVue.configs['flat/recommended'],
  vueTsConfigs.recommended,

  {
    name: 'app/rules',
    rules: {
      // Cho phép bỏ qua biến/tham số cố ý không dùng khi đặt tên bắt đầu bằng _
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
      // Chỉ cho console.warn/error trong mã sản phẩm
      'no-console': ['warn', { allow: ['warn', 'error', 'info'] }],
      // Tên component nhiều từ: các view định tuyến không cần theo quy tắc này
      'vue/multi-word-component-names': 'off',
      // Với prop khai báo bằng TypeScript, dấu '?' đã nói rõ undefined là giá
      // trị hợp lệ — ép đặt default chỉ tạo giá trị giả không ai dùng.
      'vue/require-default-prop': 'off',
    },
  },

  // skip-formatting phải đứng cuối để tắt các rule đụng Prettier
  skipFormatting,
)
