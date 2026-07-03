<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { ApiError } from '@personal-blog/api-client'

import { api } from '../services/api'
import { useAuthStore } from '../stores/auth'
import type { AdminCommentItem, AdminCommentPage } from '../types/interaction'

const auth = useAuthStore()
const comments = ref<AdminCommentItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const status = ref('PENDING')
const type = ref('ARTICLE')
const keyword = ref('')
const loading = ref(false)
const error = ref('')
const pages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const canWrite = computed(() => auth.user?.role === 'ADMIN')

const statusLabel: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  SPAM: '垃圾',
  HIDDEN: '已隐藏'
}

const load = async () => {
  loading.value = true
  error.value = ''
  const params = new URLSearchParams({ page: String(page.value), pageSize: String(pageSize) })
  if (status.value) params.set('status', status.value)
  if (type.value) params.set('type', type.value)
  if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
  try {
    const response = await api.get<AdminCommentPage>(`/admin/comments?${params}`)
    comments.value = response.items
    total.value = response.total
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '评论列表加载失败'
  } finally {
    loading.value = false
  }
}

const filter = () => {
  page.value = 1
  void load()
}

const act = async (comment: AdminCommentItem, action: 'approve' | 'reject' | 'spam' | 'hide') => {
  if (!canWrite.value) return
  error.value = ''
  try {
    await api.post(`/admin/comments/${comment.id}/${action}`)
    await load()
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '审核操作失败'
  }
}

const reply = async (comment: AdminCommentItem) => {
  if (!canWrite.value) return
  const content = window.prompt(`回复 ${comment.nickname}：`)
  if (!content?.trim()) return
  try {
    await api.post(`/admin/comments/${comment.id}/reply`, { content: content.trim() })
    await load()
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '回复失败'
  }
}

const remove = async (comment: AdminCommentItem) => {
  if (!canWrite.value || !window.confirm('确认删除这条互动？')) return
  try {
    await api.delete(`/admin/comments/${comment.id}`)
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
  <div class="content-page">
    <header class="content-header">
      <div>
        <RouterLink class="back-link" to="/">返回总览</RouterLink>
        <p class="kicker">COMMUNITY / MODERATION</p>
        <h1>评论审核</h1>
      </div>
      <div class="status"><span />{{ total }} ITEMS</div>
    </header>

    <section class="toolbar">
      <input v-model="keyword" type="search" placeholder="昵称或内容" @keyup.enter="filter">
      <select v-model="status" @change="filter">
        <option value="">全部状态</option>
        <option value="PENDING">待审核</option>
        <option value="APPROVED">已通过</option>
        <option value="REJECTED">已拒绝</option>
        <option value="SPAM">垃圾</option>
        <option value="HIDDEN">已隐藏</option>
      </select>
      <button type="button" @click="filter">筛选</button>
      <span>{{ total }} 条评论</span>
    </section>

    <p v-if="error" class="page-error">{{ error }}</p>

    <section class="moderation-list" :class="{ loading }">
      <div v-if="!loading && comments.length === 0" class="empty-state">
        <b>审核队列是空的。</b>
        <span>此刻风平浪静，没有等待处理的互动。</span>
      </div>
      <article v-for="comment in comments" :key="comment.id" class="moderation-card">
        <div class="moderation-meta">
          <span class="status-pill" :data-status="comment.status">{{ statusLabel[comment.status] }}</span>
          <b>文章评论</b>
          <span>{{ comment.articleTitle || '文章已归档' }}</span>
          <time>{{ new Date(comment.createdAt).toLocaleString() }}</time>
        </div>
        <div class="moderation-content">
          <h2>{{ comment.nickname }} <small>{{ comment.ipSummary }}</small></h2>
          <p>{{ comment.contentMarkdown }}</p>
          <a v-if="comment.website" :href="comment.website" target="_blank" rel="noopener noreferrer">
            {{ comment.website }}
          </a>
        </div>
        <div class="row-actions moderation-actions">
          <button v-if="comment.status !== 'APPROVED'" @click="act(comment, 'approve')">通过</button>
          <button v-if="comment.status !== 'REJECTED'" @click="act(comment, 'reject')">拒绝</button>
          <button @click="act(comment, 'spam')">标记垃圾</button>
          <button v-if="comment.status === 'APPROVED'" @click="act(comment, 'hide')">隐藏</button>
          <button @click="reply(comment)">回复</button>
          <button class="danger" @click="remove(comment)">删除</button>
        </div>
      </article>
    </section>

    <nav v-if="pages > 1" class="pagination">
      <button :disabled="page === 1" @click="turnPage(page - 1)">上一页</button>
      <span>{{ page }} / {{ pages }}</span>
      <button :disabled="page === pages" @click="turnPage(page + 1)">下一页</button>
    </nav>
  </div>
</template>
