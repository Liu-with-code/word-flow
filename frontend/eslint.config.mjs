import js from '@eslint/js';
import { base as aliBase } from 'eslint-config-ali';
import pluginVue from 'eslint-plugin-vue';
import tseslint from 'typescript-eslint';

/**
 * 浏览器环境全局变量声明（前端运行时）。
 * eslint-config-ali 不预设运行环境，需按项目实际使用显式声明，避免 no-undef 误报。
 */
const browserGlobals = {
  window: 'readonly',
  document: 'readonly',
  navigator: 'readonly',
  location: 'readonly',
  localStorage: 'readonly',
  sessionStorage: 'readonly',
  history: 'readonly',
  fetch: 'readonly',
  console: 'readonly',
  setTimeout: 'readonly',
  clearTimeout: 'readonly',
  setInterval: 'readonly',
  clearInterval: 'readonly',
  requestAnimationFrame: 'readonly',
  cancelAnimationFrame: 'readonly',
  matchMedia: 'readonly',
  File: 'readonly',
  FileReader: 'readonly',
  Blob: 'readonly',
  FormData: 'readonly',
  URL: 'readonly',
  URLSearchParams: 'readonly',
  Image: 'readonly',
  HTMLElement: 'readonly',
  Event: 'readonly',
  CustomEvent: 'readonly',
  AbortController: 'readonly',
  TextDecoder: 'readonly',
  TextEncoder: 'readonly',
  getComputedStyle: 'readonly',
};

/**
 * ESLint 扁平配置：规则来自 eslint-config-ali、typescript-eslint 与 eslint-plugin-vue，
 * 代码格式化由 Prettier 负责。
 */
export default tseslint.config(
  {
    ignores: ['dist/**', 'node_modules/**', 'public/**', '*.d.ts'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...aliBase,
  ...pluginVue.configs['flat/recommended'],
  {
    files: ['**/*.{ts,mts,vue}'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser,
        ecmaVersion: 'latest',
        sourceType: 'module',
      },
      globals: browserGlobals,
    },
    rules: {
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-debugger': 'error',
      // 允许用 `void promise` 标记有意不等待的异步调用
      'no-void': ['error', { allowAsStatement: true }],
      // 无返回值接口以 apiPost<void> 表达
      '@typescript-eslint/no-invalid-void-type': [
        'error',
        { allowInGenericTypeArguments: true, allowAsThisParameter: false },
      ],

      // 模板与格式类规则交由 Prettier 负责，避免两边互相覆盖
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/html-self-closing': 'off',
      'vue/html-indent': 'off',
      'vue/html-closing-bracket-newline': 'off',
      'vue/html-closing-bracket-spacing': 'off',
      'vue/attributes-order': 'off',
      '@stylistic/function-paren-newline': 'off',
      '@stylistic/max-len': 'off',
      '@stylistic/member-delimiter-style': 'off',
      // 视图组件采用 XxxView 命名，允许单词组件名
      'vue/multi-word-component-names': 'off',
      // 「先判断 ref、再 await 后赋值」被该规则误判为竞态，故关闭
      'require-atomic-updates': 'off',
    },
  },
  {
    // defineEmits/defineProps 类型签名中的参数名仅用于自文档，不参与运行
    files: ['**/*.vue'],
    rules: {
      'no-unused-vars': 'off',
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
    },
  },
  {
    // 构建脚本与配置文件运行在 Node 环境
    files: ['*.{js,mjs,cjs,ts}', '**/*.config.{js,mjs,cjs,ts}'],
    languageOptions: {
      globals: {
        process: 'readonly',
        __dirname: 'readonly',
        console: 'readonly',
      },
    },
  },
);
