<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { ApiError } from '@personal-blog/api-client'

import { api } from '../services/api'
import { useAuthStore } from '../stores/auth'
import type { ArticleListItem, ArticlePage, ArticleStatus } from '../types/article'

const router = useRouter()
const auth = useAuthStore()
const articles = ref<ArticleListItem[]>([])
const status = ref('')
const keyword = ref('')
const loading = ref(false)
const error = ref('')
const page = ref(1)
const pageSize = 20
const total = ref(0)

const pages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const canWrite = computed(() => auth.user?.role === 'ADMIN')

const statusLabel: Record<ArticleStatus, string> = {
  DRAFT: '草稿',
  SCHEDULED: '定时',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档'
}

const load = async () => {
  loading.value = true
  error.value = ''
  const params = new URLSearchParams({
    page: String(page.value),
    pageSize: String(pageSize)
  })
  if (status.value) params.set('status', status.value)
  if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
  try {
    const response = await api.get<ArticlePage>(`/admin/articles?${params}`)
    articles.value = response.items
    total.value = response.total
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '文章列表加载失败'
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  void load()
}

const turnPage = (next: number) => {
  page.value = next
  void load()
}

const changeState = async (article: ArticleListItem, action: 'publish' | 'withdraw') => {
  if (!canWrite.value) return
  error.value = ''
  try {
    await api.post(`/admin/articles/${article.id}/${action}`)
    await load()
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '操作失败'
  }
}

const remove = async (article: ArticleListItem) => {
  if (!canWrite.value || !window.confirm(`确认删除《${article.title}》？此操作会将文章移入归档。`)) return
  try {
    await api.delete(`/admin/articles/${article.id}`)
    await load()
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '删除失败'
  }
}

onMounted(load)
</script>

<template>
  <div class="content-page">
    <header class="content-header">
      <div>
        <RouterLink class="back-link" to="/">← 返回总览</RouterLink>
        <p class="kicker">CONTENT / ARTICLES</p>
        <h1>文章管理</h1>
      </div>
      <button
        v-if="canWrite"
        class="primary-action"
        type="button"
        @click="router.push('/articles/new')"
      >
        新建草稿
      </button>
    </header>

    <section class="toolbar">
      <input
        v-model="keyword"
        type="search"
        placeholder="搜索标题或 slug"
        @keyup.enter="search"
      >
      <select v-model="status" @change="search">
        <option value="">全部状态</option>
        <option value="DRAFT">草稿</option>
        <option value="PUBLISHED">已发布</option>
        <option value="SCHEDULED">定时</option>
        <option value="ARCHIVED">归档</option>
      </select>
      <button type="button" @click="search">筛选</button>
      <span>{{ total }} 篇文章</span>
    </section>

    <p v-if="error" class="page-error">{{ error }}</p>

    <section class="article-table" :class="{ loading }">
      <div class="article-row article-row-head">
        <span>文章</span>
        <span>状态</span>
        <span>分类</span>
        <span>更新于</span>
        <span>操作</span>
      </div>
      <div v-if="!loading && articles.length === 0" class="empty-state">
        <b>这里还没有文章。</b>
        <span>从一篇草稿开始，慢慢把它写成你的网站。</span>
      </div>
      <div v-for="article in articles" :key="article.id" class="article-row">
        <div>
          <strong>{{ article.title }}</strong>
          <small>/{{ article.slug }} · {{ article.wordCount }} 字</small>
        </div>
        <span class="status-pill" :data-status="article.status">
          {{ statusLabel[article.status] }}
        </span>
        <span>{{ article.categoryName || '未分类' }}</span>
        <span>{{ new Date(article.updatedAt).toLocaleString() }}</span>
        <div class="row-actions">
          <RouterLink :to="`/articles/${article.id}`">编辑</RouterLink>
          <button
            v-if="canWrite && article.status !== 'PUBLISHED'"
            type="button"
            @click="changeState(article, 'publish')"
          >
            发布
          </button>
          <button
            v-if="canWrite && article.status === 'PUBLISHED'"
            type="button"
            @click="changeState(article, 'withdraw')"
          >
            撤回
          </button>
          <button v-if="canWrite" class="danger" type="button" @click="remove(article)">
            删除
          </button>
        </div>
      </div>
    </section>

    <nav v-if="pages > 1" class="pagination" aria-label="文章分页">
      <button :disabled="page === 1" type="button" @click="turnPage(page - 1)">上一页</button>
      <span>{{ page }} / {{ pages }}</span>
      <button :disabled="page === pages" type="button" @click="turnPage(page + 1)">下一页</button>
    </nav>
  </div>
</template>
