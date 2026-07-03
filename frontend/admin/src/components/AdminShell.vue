<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import addIcon from '@fluentui/svg-icons/icons/add_24_regular.svg?url'
import commentIcon from '@fluentui/svg-icons/icons/comment_24_regular.svg?url'
import documentIcon from '@fluentui/svg-icons/icons/document_24_regular.svg?url'
import folderIcon from '@fluentui/svg-icons/icons/folder_24_regular.svg?url'
import historyIcon from '@fluentui/svg-icons/icons/history_24_regular.svg?url'
import homeIcon from '@fluentui/svg-icons/icons/home_24_regular.svg?url'
import imageIcon from '@fluentui/svg-icons/icons/image_24_regular.svg?url'
import personIcon from '@fluentui/svg-icons/icons/person_24_regular.svg?url'
import searchIcon from '@fluentui/svg-icons/icons/search_24_regular.svg?url'
import signOutIcon from '@fluentui/svg-icons/icons/sign_out_24_regular.svg?url'
import tagIcon from '@fluentui/svg-icons/icons/tag_24_regular.svg?url'
import weatherMoonIcon from '@fluentui/svg-icons/icons/weather_moon_24_regular.svg?url'
import weatherSunnyIcon from '@fluentui/svg-icons/icons/weather_sunny_24_regular.svg?url'

import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const search = ref('')
const accountOpen = ref(false)
const accountMenu = ref<HTMLElement | null>(null)
const storedTheme = localStorage.getItem('admin-theme')
const dark = ref(storedTheme === 'dark')

const navigation = [
  { label: '总览', to: '/', icon: homeIcon, match: ['dashboard'] },
  { label: '文章', to: '/articles', icon: documentIcon, match: ['articles', 'article-new', 'article-edit'] },
  { label: '分类', to: '/categories', icon: folderIcon, match: ['categories'] },
  { label: '标签', to: '/tags', icon: tagIcon, match: ['tags'] },
  { label: '媒体', to: '/media', icon: imageIcon, match: ['media'] },
  { label: '评论审核', to: '/comments', icon: commentIcon, match: ['comments'] },
  { label: '操作日志', to: '/operation-logs', icon: historyIcon, match: ['operation-logs'] }
]

const activeName = computed(() => String(route.name ?? ''))
const displayName = computed(() => auth.user?.nickname || auth.user?.username || 'Administrator')

const applyTheme = () => {
  document.documentElement.dataset.adminTheme = dark.value ? 'dark' : 'light'
  localStorage.setItem('admin-theme', dark.value ? 'dark' : 'light')
}

const toggleTheme = () => {
  dark.value = !dark.value
  applyTheme()
}

const runSearch = async () => {
  const keyword = search.value.trim()
  await router.push({ name: 'articles', query: keyword ? { keyword } : {} })
}

const logout = async () => {
  accountOpen.value = false
  await auth.logout()
  await router.replace('/login')
}

const closeAccountMenu = (event: MouseEvent) => {
  if (!accountMenu.value?.contains(event.target as Node)) accountOpen.value = false
}

onMounted(() => {
  applyTheme()
  document.addEventListener('click', closeAccountMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeAccountMenu)
})
</script>

<template>
  <div class="admin-app">
    <header class="command-bar">
      <RouterLink class="command-brand" to="/" aria-label="返回管理总览">
        <span class="command-brand-mark">C</span>
        <strong>CageWang‘s Blog</strong>
      </RouterLink>

      <nav class="command-nav" aria-label="管理后台主导航">
        <RouterLink
          v-for="item in navigation"
          :key="item.to"
          :to="item.to"
          :class="{ active: item.match.includes(activeName) }"
        >
          <img :src="item.icon" alt="">
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="command-tools">
        <form class="command-search" role="search" @submit.prevent="runSearch">
          <button class="command-search-submit" type="submit" aria-label="执行搜索">
            <img :src="searchIcon" alt="">
          </button>
          <input v-model="search" type="search" placeholder="搜索文章…" aria-label="搜索文章">
        </form>
        <button
          class="icon-button"
          type="button"
          :aria-label="dark ? '切换到浅色模式' : '切换到深色模式'"
          :title="dark ? '浅色模式' : '深色模式'"
          @click="toggleTheme"
        >
          <img :src="dark ? weatherSunnyIcon : weatherMoonIcon" alt="">
        </button>
        <div ref="accountMenu" class="account-menu">
          <button
            class="account-trigger"
            type="button"
            :aria-expanded="accountOpen"
            aria-haspopup="menu"
            @click.stop="accountOpen = !accountOpen"
          >
            <span class="account-avatar"><img :src="personIcon" alt=""></span>
            <span>{{ displayName }}</span>
          </button>
          <div v-if="accountOpen" class="account-popover" role="menu">
            <a href="http://localhost/" role="menuitem">查看公开站</a>
            <button type="button" role="menuitem" @click="logout">
              <img :src="signOutIcon" alt="">
              退出登录
            </button>
          </div>
        </div>
        <RouterLink class="primary-action command-create" to="/articles/new">
          <img :src="addIcon" alt="">
          新建文章
        </RouterLink>
      </div>
    </header>

    <main class="admin-workspace">
      <slot />
    </main>
  </div>
</template>
