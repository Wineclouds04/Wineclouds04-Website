<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { ApiError } from '@personal-blog/api-client'

import { api } from '../services/api'
import { useAuthStore } from '../stores/auth'

type SiteStatistics = {
  onlineVisitors: number
  todayViews: number
  totalViews: number
  totalVisitors: number
}

const auth = useAuthStore()
const canWrite = computed(() => auth.user?.role === 'ADMIN')
const form = reactive<SiteStatistics>({
  onlineVisitors: 0,
  todayViews: 0,
  totalViews: 0,
  totalVisitors: 0
})
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const success = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    Object.assign(form, await api.get<SiteStatistics>('/admin/statistics'))
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '站点统计加载失败'
  } finally {
    loading.value = false
  }
}

const save = async () => {
  if (!canWrite.value || saving.value) return
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    Object.assign(form, await api.put<SiteStatistics>('/admin/statistics', form))
    success.value = '统计展示值已保存，真实访问数据仍会继续自动累加。'
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '站点统计保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="content-page statistics-settings-page">
    <header class="content-header">
      <div>
        <RouterLink class="back-link" to="/">返回总览</RouterLink>
        <p class="kicker">SITE / STATISTICS</p>
        <h1>站点统计</h1>
      </div>
    </header>

    <p v-if="error" class="page-error">{{ error }}</p>
    <p v-if="success" class="profile-success">{{ success }}</p>

    <form v-if="!loading" class="profile-settings-form statistics-settings-form" @submit.prevent="save">
      <p class="statistics-settings-note">
        保存的是当前公开站展示值。后续真实访问、浏览和在线人数会在此基础上继续变化。
      </p>

      <div class="statistics-settings-grid">
        <label>
          <span>在线访客</span>
          <input v-model.number="form.onlineVisitors" :disabled="!canWrite" type="number" min="0" max="1000000000000" step="1">
        </label>
        <label>
          <span>今日浏览量</span>
          <input v-model.number="form.todayViews" :disabled="!canWrite" type="number" min="0" max="1000000000000" step="1">
        </label>
        <label>
          <span>总浏览量</span>
          <input v-model.number="form.totalViews" :disabled="!canWrite" type="number" min="0" max="1000000000000" step="1">
        </label>
        <label>
          <span>总访客量</span>
          <input v-model.number="form.totalVisitors" :disabled="!canWrite" type="number" min="0" max="1000000000000" step="1">
        </label>
      </div>

      <small>总浏览量不能小于今日浏览量。</small>

      <div class="profile-form-actions">
        <button class="primary-action" type="submit" :disabled="!canWrite || saving">
          {{ saving ? '保存中…' : '保存统计' }}
        </button>
      </div>
    </form>
  </div>
</template>
