<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { ApiError } from '@personal-blog/api-client'

import { api } from '../services/api'
import { useAuthStore } from '../stores/auth'
import type { MediaAsset, MediaPage } from '../types/media'

const auth = useAuthStore()
const canWrite = computed(() => auth.user?.role === 'ADMIN')
const configured = ref(false)
const maxImageSize = ref(10 * 1024 * 1024)
const assets = ref<MediaAsset[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 24
const loading = ref(false)
const uploading = ref(false)
const error = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

const pages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const [config, result] = await Promise.all([
      api.get<{ configured: boolean, maxImageSize: number }>('/admin/media/config'),
      api.get<MediaPage>(`/admin/media?page=${page.value}&pageSize=${pageSize}`)
    ])
    configured.value = config.configured
    maxImageSize.value = config.maxImageSize
    assets.value = result.items
    total.value = result.total
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '媒体加载失败'
  } finally {
    loading.value = false
  }
}

const chooseFile = () => fileInput.value?.click()

const upload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || uploading.value) return
  if (file.size > maxImageSize.value) {
    error.value = `图片不能超过 ${Math.round(maxImageSize.value / 1024 / 1024)} MB`
    return
  }
  const body = new FormData()
  body.append('file', file)
  body.append('altText', file.name.replace(/\.[^.]+$/, ''))
  uploading.value = true
  error.value = ''
  try {
    await api.postForm<MediaAsset>('/admin/media', body)
    page.value = 1
    await load()
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '上传失败'
  } finally {
    uploading.value = false
  }
}

const copyUrl = async (asset: MediaAsset) => {
  await navigator.clipboard.writeText(asset.url)
}

const editAltText = async (asset: MediaAsset) => {
  if (!canWrite.value) return
  const value = window.prompt('图片替代文本（用于无障碍与图片加载失败时）', asset.altText ?? '')
  if (value === null) return
  try {
    await api.put(`/admin/media/${asset.id}`, { altText: value })
    await load()
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '替代文本保存失败'
  }
}

const remove = async (asset: MediaAsset) => {
  if (!canWrite.value || !window.confirm(`确认删除图片“${asset.originalName}”？`)) return
  try {
    await api.delete(`/admin/media/${asset.id}`)
    await load()
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '删除失败'
  }
}

const turnPage = (next: number) => {
  page.value = next
  void load()
}

onMounted(load)
</script>

<template>
  <div class="content-page media-page">
    <header class="content-header">
      <div>
        <RouterLink class="back-link" to="/">← 返回总览</RouterLink>
        <p class="kicker">CONTENT / MEDIA</p>
        <h1>媒体资源</h1>
      </div>
      <div>
        <input
          ref="fileInput"
          class="visually-hidden"
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          @change="upload"
        >
        <button
          v-if="canWrite"
          class="primary-action"
          type="button"
          :disabled="uploading || !configured"
          @click="chooseFile"
        >
          {{ uploading ? '上传中…' : '上传图片' }}
        </button>
      </div>
    </header>

    <p v-if="!configured" class="config-notice">
      OSS 尚未配置。填写服务端环境变量后即可上传；访问密钥不会发送到浏览器。
    </p>
    <p v-if="error" class="page-error">{{ error }}</p>

    <section class="media-grid" :class="{ loading }">
      <div v-if="!loading && assets.length === 0" class="empty-state">
        <b>媒体库还是空的。</b>
        <span>上传第一张图片，为文章添一点颜色。</span>
      </div>
      <article v-for="asset in assets" :key="asset.id">
        <div class="media-thumb">
          <img :src="asset.url" :alt="asset.altText || asset.originalName">
        </div>
        <strong>{{ asset.originalName }}</strong>
        <small>
          {{ asset.width }}×{{ asset.height }} ·
          {{ (asset.sizeBytes / 1024).toFixed(1) }} KB
        </small>
        <div>
          <button type="button" @click="copyUrl(asset)">复制链接</button>
          <button v-if="canWrite" type="button" @click="editAltText(asset)">编辑说明</button>
          <button
            v-if="canWrite"
            class="danger"
            type="button"
            @click="remove(asset)"
          >
            删除
          </button>
        </div>
      </article>
    </section>

    <nav v-if="pages > 1" class="pagination" aria-label="媒体分页">
      <button :disabled="page === 1" type="button" @click="turnPage(page - 1)">上一页</button>
      <span>{{ page }} / {{ pages }}</span>
      <button :disabled="page === pages" type="button" @click="turnPage(page + 1)">下一页</button>
    </nav>
  </div>
</template>
