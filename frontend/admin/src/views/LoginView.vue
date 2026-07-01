<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@personal-blog/api-client'

import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const submitting = ref(false)
const error = ref('')

const submit = async () => {
  if (!username.value || !password.value || submitting.value) return
  submitting.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '登录服务暂时不可用'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-brand">
      <a class="logo" href="/">PB<span>.</span></a>
      <div>
        <p>PERSONAL BLOG / CONTROL ROOM</p>
        <h1>内容从这里，<br><em>认真发生。</em></h1>
      </div>
      <small>PHASE 02 · AUTHENTICATION</small>
    </section>

    <section class="login-panel">
      <form @submit.prevent="submit">
        <p class="eyebrow">ADMIN SIGN IN</p>
        <h2>欢迎回来</h2>
        <p class="login-hint">使用初始化管理员账号进入内容后台。</p>

        <label>
          <span>用户名</span>
          <input
            v-model.trim="username"
            name="username"
            autocomplete="username"
            maxlength="64"
            autofocus
          >
        </label>
        <label>
          <span>密码</span>
          <input
            v-model="password"
            name="password"
            type="password"
            autocomplete="current-password"
            maxlength="200"
          >
        </label>

        <p v-if="error" class="login-error" role="alert">{{ error }}</p>
        <button type="submit" :disabled="submitting || !username || !password">
          {{ submitting ? '正在验证…' : '进入控制室' }}
        </button>
      </form>
    </section>
  </main>
</template>
