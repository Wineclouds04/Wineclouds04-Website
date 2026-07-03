<script setup lang="ts">
const route = useRoute()
const theme = useTheme()
const scrolled = ref(false)

const links = [
  { label: '首页', to: '/', icon: 'icon-zhuye' },
  { label: '文章', to: '/blog', icon: 'icon-boke' },
  { label: '分类', to: '/category', icon: 'icon-folder' },
  { label: '标签', to: '/tag', icon: 'icon-biaoqian' },
  { label: '归档', to: '/archive', icon: 'icon-guidang' }
]

const isActive = (to: string) =>
  to === '/' ? route.path === '/' : route.path.startsWith(to)

const updateHeader = () => {
  scrolled.value = window.scrollY > 48
}

onMounted(() => {
  updateHeader()
  window.addEventListener('scroll', updateHeader, { passive: true })
})

onUnmounted(() => window.removeEventListener('scroll', updateHeader))
</script>

<template>
  <div class="site-shell">
    <header
      class="site-header"
      :class="{ 'over-hero': route.path === '/', scrolled }"
    >
      <div class="nav-wrap">
        <NuxtLink class="brand" to="/" aria-label="回到首页">
          <span>CageWang‘s Blog</span>
        </NuxtLink>

        <nav class="desktop-nav" aria-label="主导航">
          <NuxtLink
            v-for="link in links"
            :key="link.to"
            :to="link.to"
            :class="{ active: isActive(link.to) }"
          >
            <i class="iconfont" :class="link.icon" aria-hidden="true" />
            {{ link.label }}
          </NuxtLink>
        </nav>

        <div class="nav-actions">
          <NuxtLink class="icon-button" to="/search" aria-label="搜索文章">
            <i class="iconfont icon-sousuo" aria-hidden="true" />
          </NuxtLink>
          <button
            class="theme-button"
            type="button"
            :aria-label="theme.isDark.value ? '切换到浅色模式' : '切换到深色模式'"
            @click="theme.toggle"
          >
            {{ theme.isDark.value ? '浅色' : '深色' }}
          </button>
        </div>
      </div>

      <nav class="mobile-nav" aria-label="移动端导航">
        <NuxtLink
          v-for="link in links"
          :key="link.to"
          :to="link.to"
          :class="{ active: isActive(link.to) }"
        >
          <i class="iconfont" :class="link.icon" aria-hidden="true" />
          {{ link.label }}
        </NuxtLink>
      </nav>
    </header>

    <main>
      <NuxtPage />
    </main>

    <footer class="site-footer">
      <div>
        <p class="footer-title">CageWang‘s Blog</p>
        <p>在代码、生活和那些尚未命名的念头之间，留一点呼吸。</p>
      </div>
      <div class="footer-links">
        <NuxtLink to="/archive">归档</NuxtLink>
        <NuxtLink to="/privacy">隐私</NuxtLink>
        <a href="/rss.xml">RSS</a>
        <a href="/sitemap.xml">Sitemap</a>
      </div>
      <p class="copyright">© {{ new Date().getFullYear() }} · Built with care and Nuxt.</p>
    </footer>
  </div>
</template>
