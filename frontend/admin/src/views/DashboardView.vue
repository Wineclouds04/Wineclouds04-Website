<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '../stores/auth'

type ApiStatus = {
  application: string
  status: string
  timestamp: string
}

const apiStatus = ref<ApiStatus | null>(null)
const apiError = ref(false)
const auth = useAuthStore()
const router = useRouter()

const logout = async () => {
  await auth.logout()
  await router.replace('/login')
}

onMounted(async () => {
  try {
    const response = await fetch('/api/v1/status')
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    apiStatus.value = await response.json() as ApiStatus
  } catch {
    apiError.value = true
  }
})

const modules = [
  { name: '文章管理', state: '已开放', icon: '文', path: '/articles' },
  { name: '媒体资源', state: '已开放', icon: '图', path: '/media' },
  { name: '评论审核', state: '阶段四', icon: '评' },
  { name: '站点数据', state: '阶段五', icon: '数' }
]
</script>

<template>
  <div class="shell">
    <aside>
      <div class="logo">PB<span>.</span></div>
      <p>CONTROL ROOM</p>
      <nav>
        <a class="active" href="/">总览</a>
        <RouterLink to="/articles">内容</RouterLink>
        <RouterLink to="/categories">分类</RouterLink>
        <RouterLink to="/tags">标签</RouterLink>
        <RouterLink to="/media">媒体</RouterLink>
        <span>设置</span>
      </nav>
      <button class="aside-logout" type="button" @click="logout">退出登录</button>
      <div class="stage">PHASE 02</div>
    </aside>

    <main>
      <header>
        <div>
          <p class="kicker">CONTENT / AUTHENTICATED</p>
          <h1>你好，{{ auth.user?.nickname }}。<br>内容后台已解锁。</h1>
        </div>
        <div
          class="status"
          :class="{ error: apiError }"
        >
          <span />
          {{ apiError ? 'API OFFLINE' : apiStatus ? 'ALL SYSTEMS GO' : 'CHECKING' }}
        </div>
      </header>

      <section class="metric">
        <p>当前里程碑</p>
        <strong>02<span>/06</span></strong>
        <div>
          <b>认证与文章核心</b>
          <small>JWT会话、内容模型与编辑工作流</small>
        </div>
      </section>

      <section class="modules">
        <article
          v-for="module in modules"
          :key="module.name"
          :class="{ clickable: module.path }"
          @click="module.path && router.push(module.path)"
        >
          <div class="icon">{{ module.icon }}</div>
          <h2>{{ module.name }}</h2>
          <p>{{ module.state }}开放</p>
        </article>
      </section>

      <footer v-if="apiStatus">
        {{ apiStatus.application }} · {{ apiStatus.status.toUpperCase() }} ·
        {{ new Date(apiStatus.timestamp).toLocaleString() }}
      </footer>
    </main>
  </div>
</template>
