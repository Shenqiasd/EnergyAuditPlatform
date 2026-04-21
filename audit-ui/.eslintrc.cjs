/* eslint-env node */
/**
 * Minimal ESLint config for audit-ui.
 *
 * Intent: catch real errors (undefined vars, duplicate keys, dead code) without
 * imposing stylistic rules on existing code. Rules can be tightened incrementally
 * in follow-up PRs.
 */
module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true,
  },
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 'latest',
    sourceType: 'module',
    extraFileExtensions: ['.vue'],
  },
  plugins: ['@typescript-eslint'],
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
    'plugin:@typescript-eslint/recommended',
  ],
  rules: {
    // Style rules that would fail on existing code — disabled for now
    'vue/multi-word-component-names': 'off',
    'vue/attribute-hyphenation': 'off',
    'vue/v-on-event-hyphenation': 'off',
    'vue/html-self-closing': 'off',
    'vue/max-attributes-per-line': 'off',
    'vue/singleline-html-element-content-newline': 'off',
    'vue/html-indent': 'off',
    'vue/html-closing-bracket-newline': 'off',
    'vue/attributes-order': 'off',
    'vue/first-attribute-linebreak': 'off',
    'vue/no-v-html': 'off',
    'vue/require-default-prop': 'off',
    'vue/multiline-html-element-content-newline': 'off',
    'vue/html-closing-bracket-spacing': 'off',
    'vue/html-end-tags': 'off',
    'vue/valid-template-root': 'error',

    // Permissive TS — the existing code uses `any` extensively; we'll tighten later
    '@typescript-eslint/no-explicit-any': 'off',
    '@typescript-eslint/no-unused-vars': ['warn', {
      argsIgnorePattern: '^_',
      varsIgnorePattern: '^_',
      caughtErrorsIgnorePattern: '^_',
    }],
    '@typescript-eslint/ban-ts-comment': 'off',
    '@typescript-eslint/no-empty-function': 'off',
    '@typescript-eslint/no-non-null-assertion': 'off',

    // Real errors we DO want to catch
    'no-undef': 'error',
    'no-duplicate-case': 'error',
    'no-unreachable': 'error',
    'no-constant-condition': ['error', { checkLoops: false }],
    'no-dupe-keys': 'error',
    'no-dupe-args': 'error',
    'no-self-compare': 'error',
    'no-self-assign': 'error',

    // Downgrade to warn to avoid blocking existing code
    'no-empty': 'warn',
    'no-prototype-builtins': 'warn',
    'no-case-declarations': 'warn',
    'no-control-regex': 'off',
    'no-async-promise-executor': 'warn',
    'no-useless-escape': 'warn',
    'prefer-const': 'warn',
    '@typescript-eslint/no-this-alias': 'warn',
    '@typescript-eslint/ban-types': 'off',
    '@typescript-eslint/no-var-requires': 'off',
    '@typescript-eslint/no-empty-interface': 'off',

    // Production-only gate
    'no-console': process.env.CI === 'true' ? ['warn', { allow: ['warn', 'error', 'info'] }] : 'off',
    'no-debugger': 'error',
  },
  ignorePatterns: [
    'dist',
    'node_modules',
    'public',
    'scripts',
    '*.d.ts',
    'src/components.d.ts',
    'src/auto-imports.d.ts',
    'vite.config.ts',
  ],
  overrides: [
    {
      files: ['*.vue'],
      rules: {
        // Vue-specific loosenings
        'vue/component-definition-name-casing': 'off',
      },
    },
  ],
}
