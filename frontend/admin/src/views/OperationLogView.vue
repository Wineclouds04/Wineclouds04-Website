<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { ApiError } from '@personal-blog/api-client'

import { api } from '../services/api'
import type { OperationLogItem, OperationLogPage } from '../types/interaction'

const items = ref<OperationLogItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 30
const module = ref('')
const error = ref('')
const loading = ref(false)
const pages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const load = async () => {
  loading.value = true
  error.value = ''
  const params = new URLSearchParams({ page: String(page.value), pageSize: String(pageSize) })
  if (module.value) params.set('module', module.value)
  try {
    const result = await api.get<OperationLogPage>(`/admin/operation-logs?${params}`)
    items.value = result.items
    total.value = result.total
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '操作日志加载失败'
  } finally {
    loading.value = false
  }
}

const filter = () => {
  page.value = 1
  void load()
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
        <p class="kicker">SYSTEM / AUDIT TRAIL</p>
        <h1>操作日志</h1>
      </div>
    </header>

    <section class="toolbar">
      <select v-model="module" @change="filter">
        <option value="">全部模块</option>
        <option value="COMMENT">评论审核</option>
        <option value="ARTICLE">文章</option>
        <option value="TAXONOMY">分类标签</option>
        <option value="MEDIA">媒体</option>
      </select>
      <span>{{ total }} 条记录</span>
    </section>
    <p v-if="error" class="page-error">{{ error }}</p>

    <section class="log-table" :class="{ loading }">
      <div class="log-row log-head">
        <span>时间</span><span>操作者</span><span>模块 / 动作</span><span>对象</span><span>结果</span>
      </div>
      <div v-if="!loading && items.length === 0" class="empty-state">
        <b>还没有操作记录。</b>
        <span>后续审核与管理动作会在这里留下轨迹。</span>
      </div>
      <div v-for="item in items" :key="item.id" class="log-row">
        <time>{{ new Date(item.createdAt).toLocaleString() }}</time>
        <span>{{ item.operatorName || `用户 ${item.operatorId || '-'}` }}</span>
        <strong>{{ item.module }} / {{ item.action }}</strong>
        <code>{{ item.targetId || '-' }}</code>
        <span class="log-result">{{ item.result }}</span>
      </div>
    </section>

    <nav v-if="pages > 1" class="pagination">
      <button :disabled="page === 1" @click="turnPage(page - 1)">上一页</button>
      <span>{{ page }} / {{ pages }}</span>
      <button :disabled="page === pages" @click="turnPage(page + 1)">下一页</button>
    </nav>
  </div>
</template>
