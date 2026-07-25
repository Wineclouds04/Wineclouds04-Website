<script setup lang="ts">
import weatherMoonIcon from '@fluentui/svg-icons/icons/weather_moon_24_regular.svg?url'
import weatherSunnyIcon from '@fluentui/svg-icons/icons/weather_sunny_24_regular.svg?url'

const route = useRoute()
const theme = useTheme()
const api = useBlogApi()
const { data: profile } = await useAsyncData('header-site-profile', () => api.profile())
const isScrolled = ref(false)
const audio = ref<HTMLAudioElement | null>(null)
const playing = ref(false)
const audioFailed = ref(false)
const musicAvailable = computed(() => Boolean(profile.value?.musicEnabled && profile.value.musicUrl))
const playerCover = computed(() =>
  profile.value?.musicCoverUrl || profile.value?.avatarUrl || '/images/wineclouds-avatar.webp'
)
const playerLabel = computed(() => {
  if (!musicAvailable.value) return '尚未配置背景音乐'
  if (audioFailed.value) return '音乐暂时无法播放'
  const title = profile.value?.musicTitle || '背景音乐'
  return playing.value ? `暂停 ${title}` : `播放 ${title}`
})

const navigationLinks = [
  { label: '首页', to: '/', icon: 'icon-zhuye' },
  { label: '文章', to: '/blog', icon: 'icon-boke' },
  { label: '分类', to: '/category', icon: 'icon-folder' },
  { label: '标签', to: '/tag', icon: 'icon-biaoqian' },
  { label: '归档', to: '/archive', icon: 'icon-guidang' }
]

const isActiveRoute = (path: string) =>
  path === '/' ? route.path === '/' : route.path.startsWith(path)

const updateScrollState = () => {
  isScrolled.value = window.scrollY > 48
}

const startPlayback = async () => {
  if (!audio.value || !musicAvailable.value) return
  try {
    await audio.value.play()
    playing.value = true
    audioFailed.value = false
  } catch {
    // Browsers may block unmuted autoplay until the visitor clicks the player.
    playing.value = false
  }
}

const togglePlayback = async () => {
  if (!audio.value || audioFailed.value) return
  if (audio.value.paused) {
    await startPlayback()
  } else {
    audio.value.pause()
  }
}

onMounted(async () => {
  updateScrollState()
  window.addEventListener('scroll', updateScrollState, { passive: true })
  await nextTick()
  await startPlayback()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', updateScrollState)
})
</script>

<template>
  <header class="site-header" :class="{ scrolled: isScrolled }">
    <div class="nav-wrap">
      <NuxtLink class="brand" to="/" aria-label="回到首页">
        <span>Wineclouds’Blog</span>
      </NuxtLink>

      <nav class="desktop-nav" aria-label="主导航">
        <NuxtLink
          v-for="link in navigationLinks"
          :key="link.to"
          :to="link.to"
          :class="{ active: isActiveRoute(link.to) }"
        >
          <i class="iconfont" :class="link.icon" aria-hidden="true" />
          {{ link.label }}
        </NuxtLink>
      </nav>

      <div class="nav-actions">
        <div class="header-music-player">
          <audio
            v-if="musicAvailable"
            ref="audio"
            :src="profile?.musicUrl"
            autoplay
            loop
            preload="auto"
            @play="playing = true"
            @pause="playing = false"
            @error="audioFailed = true; playing = false"
          />
          <button
            class="header-music-button"
            :class="{ playing }"
            type="button"
            :aria-label="playerLabel"
            :title="playerLabel"
            :disabled="!musicAvailable || audioFailed"
            @click="togglePlayback"
          >
            <img :src="playerCover" alt="">
            <svg v-if="playing" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M7 5h4v14H7zm6 0h4v14h-4z" />
            </svg>
            <svg v-else viewBox="0 0 24 24" aria-hidden="true">
              <path d="m8 5 11 7-11 7z" />
            </svg>
          </button>
        </div>

        <NuxtLink class="icon-button" to="/search" aria-label="搜索文章">
          <i class="iconfont icon-sousuo" aria-hidden="true" />
        </NuxtLink>

        <button
          class="theme-button"
          type="button"
          :aria-label="theme.isDark.value ? '切换到浅色模式' : '切换到深色模式'"
          :title="theme.isDark.value ? '浅色模式' : '深色模式'"
          @click="theme.toggle"
        >
          <img
            :src="theme.isDark.value ? weatherSunnyIcon : weatherMoonIcon"
            alt=""
          >
        </button>
      </div>
    </div>

    <nav class="mobile-nav" aria-label="移动端导航">
      <NuxtLink
        v-for="link in navigationLinks"
        :key="link.to"
        :to="link.to"
        :class="{ active: isActiveRoute(link.to) }"
      >
        <i class="iconfont" :class="link.icon" aria-hidden="true" />
        {{ link.label }}
      </NuxtLink>
    </nav>
  </header>
</template>
