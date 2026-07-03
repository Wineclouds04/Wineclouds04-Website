<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { ApiError } from '@personal-blog/api-client'

import { api } from '../services/api'
import { useAuthStore } from '../stores/auth'
import type {
  ArticleDetail,
  ArticleInput,
  TaxonomyOption
} from '../types/article'
import type { MediaAsset } from '../types/media'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const id = computed(() => route.params.id ? Number(route.params.id) : null)
const isNew = computed(() => id.value === null)
const canWrite = computed(() => auth.user?.role === 'ADMIN')
const saving = ref(false)
const loading = ref(!isNew.value)
const error = ref('')
const savedAt = ref('')
const saveState = ref<'idle' | 'dirty' | 'saving' | 'saved' | 'error'>('idle')
const previewHtml = ref('')
const previewStats = reactive({ wordCount: 0, readingMinutes: 0 })
const previewing = ref(false)
const imageUploading = ref(false)
const ossConfigured = ref(false)
const imageInput = ref<HTMLInputElement | null>(null)
const ready = ref(false)
let saveTimer: ReturnType<typeof setTimeout> | undefined
let previewTimer: ReturnType<typeof setTimeout> | undefined
let savedFingerprint = ''
const categories = ref<TaxonomyOption[]>([])
const tags = ref<TaxonomyOption[]>([])

const form = reactive<ArticleInput>({
  title: '',
  slug: '',
  summary: '',
  contentMarkdown: '',
  categoryId: null,
  tagIds: [],
  visibility: 'PUBLIC',
  pinned: false,
  allowComment: true,
  metaTitle: '',
  metaDescription: '',
  canonicalUrl: '',
  version: 0
})

const applyArticle = (article: ArticleDetail) => {
  Object.assign(form, {
    title: article.title,
    slug: article.slug,
    summary: article.summary ?? '',
    contentMarkdown: article.contentMarkdown,
    categoryId: article.categoryId ?? null,
    tagIds: article.tagIds,
    visibility: article.visibility,
    pinned: article.pinned,
    allowComment: article.allowComment,
    metaTitle: article.metaTitle ?? '',
    metaDescription: article.metaDescription ?? '',
    canonicalUrl: article.canonicalUrl ?? '',
    version: article.version
  })
  savedFingerprint = JSON.stringify(form)
}

const updatePreview = async () => {
  previewing.value = true
  try {
    const result = await api.post<{
      html: string
      wordCount: number
      readingMinutes: number
    }>('/admin/markdown/preview', { markdown: form.contentMarkdown })
    previewHtml.value = result.html
    previewStats.wordCount = result.wordCount
    previewStats.readingMinutes = result.readingMinutes
  } catch {
    // Preview failures should not destroy the draft or interrupt editing.
  } finally {
    previewing.value = false
  }
}

const schedulePreview = () => {
  clearTimeout(previewTimer)
  previewTimer = setTimeout(() => void updatePreview(), 350)
}

const load = async () => {
  error.value = ''
  try {
    const [categoryOptions, tagOptions, mediaConfig] = await Promise.all([
      api.get<TaxonomyOption[]>('/admin/categories/options'),
      api.get<TaxonomyOption[]>('/admin/tags/options'),
      api.get<{ configured: boolean }>('/admin/media/config')
    ])
    categories.value = categoryOptions
    tags.value = tagOptions
    ossConfigured.value = mediaConfig.configured
    if (id.value !== null) {
      applyArticle(await api.get<ArticleDetail>(`/admin/articles/${id.value}`))
    }
    await updatePreview()
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '文章加载失败'
  } finally {
    loading.value = false
    ready.value = true
  }
}

const chooseImage = () => imageInput.value?.click()

const uploadImage = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || imageUploading.value) return
  const alt = file.name.replace(/\.[^.]+$/, '')
  const body = new FormData()
  body.append('file', file)
  body.append('altText', alt)
  imageUploading.value = true
  error.value = ''
  try {
    const asset = await api.postForm<MediaAsset>('/admin/media', body)
    const separator = form.contentMarkdown && !form.contentMarkdown.endsWith('\n') ? '\n\n' : ''
    form.contentMarkdown += `${separator}![${asset.altText || alt}](${asset.url})\n`
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '图片上传失败'
  } finally {
    imageUploading.value = false
  }
}

const save = async (automatic = false) => {
  if (!canWrite.value) return
  if (saving.value) {
    if (automatic) {
      clearTimeout(saveTimer)
      saveTimer = setTimeout(() => void save(true), 1000)
    }
    return
  }
  error.value = ''
  if (!form.title.trim() || !form.slug.trim()) {
    if (!automatic) error.value = '标题和 slug 不能为空'
    return
  }
  saving.value = true
  saveState.value = 'saving'
  try {
    const wasNew = isNew.value
    const payload = { ...form, tagIds: [...form.tagIds] }
    const sentFingerprint = JSON.stringify(payload)
    const article = wasNew
      ? await api.post<ArticleDetail>('/admin/articles', payload)
      : await api.put<ArticleDetail>(`/admin/articles/${id.value}`, payload)
    if (JSON.stringify(form) === sentFingerprint) {
      applyArticle(article)
      saveState.value = 'saved'
    } else {
      form.version = article.version
      saveState.value = 'dirty'
    }
    savedAt.value = new Date().toLocaleTimeString()
    if (wasNew) await router.replace(`/articles/${article.id}`)
    if (saveState.value === 'dirty') {
      clearTimeout(saveTimer)
      saveTimer = setTimeout(() => void save(true), 500)
    }
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '保存失败'
    saveState.value = 'error'
  } finally {
    saving.value = false
  }
}

watch(
  form,
  () => {
    if (!ready.value) return
    if (JSON.stringify(form) === savedFingerprint) return
    saveState.value = 'dirty'
    schedulePreview()
    if (!isNew.value && canWrite.value) {
      clearTimeout(saveTimer)
      saveTimer = setTimeout(() => void save(true), 1800)
    }
  },
  { deep: true }
)

onMounted(load)
onBeforeUnmount(() => {
  clearTimeout(saveTimer)
  clearTimeout(previewTimer)
})
</script>

<template>
  <div class="editor-page">
    <header class="editor-header">
      <div>
        <RouterLink class="back-link" to="/articles">返回文章列表</RouterLink>
        <p class="kicker">CONTENT / {{ isNew ? 'NEW DRAFT' : 'EDIT' }}</p>
      </div>
      <div class="editor-actions">
        <small v-if="saveState === 'dirty'">等待自动保存</small>
        <small v-else-if="saveState === 'saving'">正在保存…</small>
        <small v-else-if="saveState === 'error'">自动保存失败</small>
        <small v-else-if="savedAt">已保存于 {{ savedAt }}</small>
        <button
          class="primary-action"
          type="button"
          :disabled="saving || !canWrite"
          @click="save(false)"
        >
          {{ saving ? '保存中…' : isNew ? '创建草稿' : '保存更改' }}
        </button>
      </div>
    </header>

    <p v-if="error" class="page-error">{{ error }}</p>
    <div v-if="loading" class="editor-loading">正在打开稿纸…</div>

    <form v-else class="article-editor" @submit.prevent="save(false)">
      <main>
        <input
          v-model="form.title"
          class="title-input"
          type="text"
          maxlength="200"
          placeholder="文章标题"
          :readonly="!canWrite"
        >
        <label>
          <span>URL SLUG</span>
          <div class="slug-field">
            <i>/article/</i>
            <input
              v-model="form.slug"
              type="text"
              maxlength="160"
              pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
              placeholder="my-first-article"
              :readonly="!canWrite"
            >
          </div>
        </label>
        <label>
          <span>摘要</span>
          <textarea
            v-model="form.summary"
            rows="3"
            maxlength="600"
            placeholder="用一两句话告诉读者这篇文章讲什么。"
            :readonly="!canWrite"
          />
        </label>
        <div class="markdown-workbench">
          <label class="markdown-field">
            <span class="markdown-label">
              MARKDOWN 正文
              <input
                ref="imageInput"
                class="visually-hidden"
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                @change="uploadImage"
              >
              <button
                v-if="canWrite"
                type="button"
                :disabled="imageUploading || !ossConfigured"
                @click="chooseImage"
              >
                {{ imageUploading ? '上传中…' : ossConfigured ? '插入图片' : 'OSS 未配置' }}
              </button>
            </span>
            <textarea
              v-model="form.contentMarkdown"
              rows="24"
              placeholder="# 从这里开始写…"
              :readonly="!canWrite"
            />
          </label>
          <section class="markdown-preview">
            <div>
              <span>实时预览</span>
              <small>
                {{ previewing ? '渲染中…' : `${previewStats.wordCount} 字 · ${previewStats.readingMinutes} 分钟` }}
              </small>
            </div>
            <!-- HTML is rendered and sanitized by the backend Markdown service. -->
            <article v-if="previewHtml" v-html="previewHtml" />
            <p v-else>预览会在你开始写作后出现。</p>
          </section>
        </div>
      </main>

      <aside>
        <section>
          <h2>发布设置</h2>
          <label>
            <span>可见性</span>
            <select v-model="form.visibility" :disabled="!canWrite">
              <option value="PUBLIC">公开</option>
              <option value="PRIVATE">私密</option>
            </select>
          </label>
          <label>
            <span>分类</span>
            <select v-model="form.categoryId" :disabled="!canWrite">
              <option :value="null">未分类</option>
              <option v-for="item in categories" :key="item.id" :value="item.id">
                {{ item.name }}
              </option>
            </select>
          </label>
          <fieldset>
            <legend>标签</legend>
            <label v-for="item in tags" :key="item.id" class="check-row">
              <input
                v-model="form.tagIds"
                type="checkbox"
                :value="item.id"
                :disabled="!canWrite"
              >
              <span>{{ item.name }}</span>
            </label>
            <small v-if="tags.length === 0">暂无标签，可稍后在标签管理中添加。</small>
          </fieldset>
          <label class="check-row">
            <input v-model="form.pinned" type="checkbox" :disabled="!canWrite">
            <span>置顶文章</span>
          </label>
          <label class="check-row">
            <input v-model="form.allowComment" type="checkbox" :disabled="!canWrite">
            <span>允许评论</span>
          </label>
        </section>

        <section>
          <h2>SEO</h2>
          <label>
            <span>SEO 标题</span>
            <input v-model="form.metaTitle" maxlength="200" :readonly="!canWrite">
          </label>
          <label>
            <span>SEO 描述</span>
            <textarea
              v-model="form.metaDescription"
              rows="4"
              maxlength="320"
              :readonly="!canWrite"
            />
          </label>
          <label>
            <span>Canonical URL</span>
            <input v-model="form.canonicalUrl" maxlength="500" :readonly="!canWrite">
          </label>
        </section>
      </aside>
    </form>
  </div>
</template>
