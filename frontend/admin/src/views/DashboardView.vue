<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { ApiError } from '@personal-blog/api-client'

import { api } from '../services/api'
import { useAuthStore } from '../stores/auth'
import type { DashboardResponse } from '../types/interaction'

const dashboard = ref<DashboardResponse | null>(null)
const apiError = ref('')
const auth = useAuthStore()

const maxTrend = computed(() =>
  Math.max(1, ...(dashboard.value?.trend.map(item => item.views) || [1]))
)

const metrics = computed(() => {
  const data = dashboard.value
  return [
    { label: '全部文章', value: data?.articles ?? 0, note: `${data?.publishedArticles ?? 0} 篇已发布` },
    { label: '待审核评论', value: data?.pendingComments ?? 0, note: `${data?.comments ?? 0} 条全部评论` },
    { label: '累计喜欢', value: data?.likes ?? 0, note: '匿名访客去重' },
    { label: '30 天访客', value: data?.visitors30d ?? 0, note: `${data?.views30d ?? 0} 次浏览` }
  ]
})

onMounted(async () => {
  try {
    dashboard.value = await api.get<DashboardResponse>('/admin/dashboard')
  } catch (cause) {
    apiError.value = cause instanceof ApiError ? cause.message : '仪表盘加载失败'
  }
})
</script>

<template>
  <div class="dashboard-page">
    <header class="dashboard-header">
      <div>
        <p class="kicker">OVERVIEW / LIVE DATA</p>
        <h1>你好，{{ auth.user?.nickname || auth.user?.username }}。</h1>
        <p class="dashboard-lead">看看网站正在发生什么。</p>
      </div>
      <div class="status" :class="{ error: apiError }">
        <span />
        {{ apiError ? '数据连接异常' : dashboard ? '所有服务正常' : '正在检查' }}
      </div>
    </header>

    <p v-if="apiError" class="page-error">{{ apiError }}</p>

    <section class="dashboard-metrics">
      <article v-for="metric in metrics" :key="metric.label">
        <p>{{ metric.label }}</p>
        <strong>{{ metric.value.toLocaleString() }}</strong>
        <small>{{ metric.note }}</small>
      </article>
    </section>

    <section class="dashboard-grid">
      <article class="trend-panel">
        <div class="panel-heading">
          <div><p>TRAFFIC / 30 DAYS</p><h2>访问趋势</h2></div>
          <span>{{ dashboard?.views30d || 0 }} PV</span>
        </div>
        <div v-if="dashboard?.trend.length" class="trend-chart">
          <div
            v-for="point in dashboard.trend"
            :key="point.date"
            class="trend-bar"
            :style="{ height: `${Math.max(5, point.views / maxTrend * 100)}%` }"
            :title="`${point.date}: ${point.views} PV / ${point.visitors} UV`"
          />
        </div>
        <div v-else class="panel-empty">开始产生访问数据后，趋势会显示在这里。</div>
      </article>

      <article class="queue-panel">
        <div class="panel-heading">
          <div><p>MODERATION</p><h2>审核队列</h2></div>
        </div>
        <strong class="queue-number">{{ dashboard?.pendingComments || 0 }}</strong>
        <p>条评论等待处理</p>
        <RouterLink class="primary-action inline-action" to="/comments">进入审核台</RouterLink>
      </article>

      <article class="popular-panel">
        <div class="panel-heading">
          <div><p>CONTENT</p><h2>热门文章</h2></div>
        </div>
        <div v-if="dashboard?.popularArticles.length" class="popular-list">
          <div v-for="(item, index) in dashboard.popularArticles" :key="item.id">
            <b>0{{ index + 1 }}</b>
            <span><strong>{{ item.title }}</strong><small>{{ item.views }} 浏览 · {{ item.likes }} 喜欢 · {{ item.comments }} 评论</small></span>
          </div>
        </div>
        <div v-else class="panel-empty">发布文章后，这里会出现内容排行。</div>
      </article>

      <article class="operations-panel">
        <div class="panel-heading">
          <div><p>AUDIT TRAIL</p><h2>最近操作</h2></div>
          <RouterLink to="/operation-logs">查看全部 →</RouterLink>
        </div>
        <div v-if="dashboard?.recentOperations.length" class="recent-list">
          <div v-for="item in dashboard.recentOperations" :key="item.id">
            <span>{{ item.module }} / {{ item.action }}</span>
            <time>{{ new Date(item.createdAt).toLocaleString() }}</time>
          </div>
        </div>
        <div v-else class="panel-empty">管理动作会安全地记录在这里。</div>
      </article>
    </section>

    <footer v-if="dashboard" class="dashboard-footer">
      服务 {{ dashboard.serviceStatus }} ·
      缓存命中 {{ Math.round(dashboard.cacheHitRate * 100) }}% ·
      邮件 {{ dashboard.mailConfigured ? '已配置' : '未配置' }} ·
      {{ dashboard.pendingNotifications }} 条通知待发送
    </footer>
  </div>
</template>
