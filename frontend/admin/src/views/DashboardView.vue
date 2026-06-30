<script setup lang="ts">
import { onMounted, ref } from 'vue'

type ApiStatus = {
  application: string
  status: string
  timestamp: string
}

const apiStatus = ref<ApiStatus | null>(null)
const apiError = ref(false)

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
  { name: '文章管理', state: '阶段二', icon: '文' },
  { name: '媒体资源', state: '阶段二', icon: '图' },
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
        <span>内容</span>
        <span>互动</span>
        <span>设置</span>
      </nav>
      <div class="stage">PHASE 01</div>
    </aside>

    <main>
      <header>
        <div>
          <p class="kicker">WEDNESDAY / INFRASTRUCTURE</p>
          <h1>早上好，<br>地基已经就绪。</h1>
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
        <strong>01<span>/06</span></strong>
        <div>
          <b>工程与基础设施</b>
          <small>Compose、迁移、网关与流水线</small>
        </div>
      </section>

      <section class="modules">
        <article v-for="module in modules" :key="module.name">
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

