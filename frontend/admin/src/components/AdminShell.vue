<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import chevronLeftIcon from '@fluentui/svg-icons/icons/chevron_left_24_regular.svg?url'
import chevronRightIcon from '@fluentui/svg-icons/icons/chevron_right_24_regular.svg?url'
import commentIcon from '@fluentui/svg-icons/icons/comment_24_regular.svg?url'
import dataBarIcon from '@fluentui/svg-icons/icons/data_bar_vertical_24_regular.svg?url'
import documentIcon from '@fluentui/svg-icons/icons/document_24_regular.svg?url'
import folderIcon from '@fluentui/svg-icons/icons/folder_24_regular.svg?url'
import gridIcon from '@fluentui/svg-icons/icons/grid_24_regular.svg?url'
import historyIcon from '@fluentui/svg-icons/icons/history_24_regular.svg?url'
import homeIcon from '@fluentui/svg-icons/icons/home_24_regular.svg?url'
import imageIcon from '@fluentui/svg-icons/icons/image_24_regular.svg?url'
import personIcon from '@fluentui/svg-icons/icons/person_24_regular.svg?url'
import signOutIcon from '@fluentui/svg-icons/icons/sign_out_24_regular.svg?url'
import tagIcon from '@fluentui/svg-icons/icons/tag_24_regular.svg?url'
import moonIcon from '@fluentui/svg-icons/icons/weather_moon_24_regular.svg?url'
import sunIcon from '@fluentui/svg-icons/icons/weather_sunny_24_regular.svg?url'

import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const collapsed = ref(localStorage.getItem('admin-sidebar-collapsed') === 'true')
const storedTheme = localStorage.getItem('blog-theme')
const adminTheme = ref(storedTheme === 'dark' || (!storedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light')

const navigation = [
  { label: '仪表盘', to: '/', icon: homeIcon, match: ['dashboard'] },
  { label: '文章管理', to: '/articles', icon: documentIcon, match: ['articles', 'article-new', 'article-edit'] },
  { label: '分类管理', to: '/categories', icon: folderIcon, match: ['categories'] },
  { label: '标签管理', to: '/tags', icon: tagIcon, match: ['tags'] },
  { label: '媒体管理', to: '/media', icon: imageIcon, match: ['media'] },
  { label: '站点资料', to: '/profile', icon: personIcon, match: ['profile'] },
  { label: '站点统计', to: '/statistics', icon: dataBarIcon, match: ['statistics'] },
  { label: '评论管理', to: '/comments', icon: commentIcon, match: ['comments'] },
  { label: '操作日志', to: '/operation-logs', icon: historyIcon, match: ['operation-logs'] }
]

const activeName = computed(() => String(route.name ?? ''))
const pageTitle = computed(() => String(route.meta.title ?? '管理'))
const displayName = computed(() => auth.user?.nickname || auth.user?.username || '管理员')
const isEditorPage = computed(() => ['article-new', 'article-edit'].includes(activeName.value))
const themeIcon = computed(() => adminTheme.value === 'dark' ? sunIcon : moonIcon)
const themeLabel = computed(() => adminTheme.value === 'dark' ? '切换为浅色模式' : '切换为深色模式')

const applyTheme = () => {
  document.documentElement.dataset.theme = adminTheme.value
  localStorage.setItem('blog-theme', adminTheme.value)
}

applyTheme()

const toggleSidebar = () => {
  collapsed.value = !collapsed.value
  localStorage.setItem('admin-sidebar-collapsed', String(collapsed.value))
}

const toggleTheme = () => {
  adminTheme.value = adminTheme.value === 'dark' ? 'light' : 'dark'
  applyTheme()
}

const logout = async () => {
  await auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="admin-shell" :class="{ 'sidebar-collapsed': collapsed }">
    <aside class="admin-sidebar" :class="{ 'is-collapsed': collapsed }">
      <RouterLink class="sidebar-brand" to="/" aria-label="返回管理总览">
        <span class="sidebar-brand-mark"><img :src="gridIcon" alt=""></span>
        <strong v-if="!collapsed">Wineclouds’Blog</strong>
      </RouterLink>

      <nav class="sidebar-nav" aria-label="管理后台主导航">
        <RouterLink
          v-for="item in navigation"
          :key="item.to"
          :to="item.to"
          class="sidebar-nav-item"
          :class="{ active: item.match.includes(activeName) }"
          :title="collapsed ? item.label : undefined"
        >
          <img :src="item.icon" alt="">
          <span v-if="!collapsed">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <button class="sidebar-collapse" type="button" :aria-label="collapsed ? '展开侧栏' : '折叠侧栏'" @click="toggleSidebar">
        <img :src="collapsed ? chevronRightIcon : chevronLeftIcon" alt="">
        <span v-if="!collapsed">折叠侧栏</span>
      </button>
    </aside>

    <section class="admin-main-wrapper">
      <header class="admin-topbar">
        <nav class="admin-breadcrumb" aria-label="面包屑">
          <RouterLink to="/">首页</RouterLink>
          <span>/</span>
          <strong>{{ pageTitle }}</strong>
        </nav>
        <div class="admin-account">
          <button
            class="theme-toggle"
            type="button"
            :aria-label="themeLabel"
            :aria-pressed="adminTheme === 'dark'"
            :title="themeLabel"
            @click="toggleTheme"
          >
            <img :src="themeIcon" alt="">
          </button>
          <span class="admin-user"><img :src="personIcon" alt="">{{ displayName }}</span>
          <button type="button" @click="logout"><img :src="signOutIcon" alt="">退出</button>
        </div>
      </header>

      <main class="admin-page-main" :class="{ 'editor-main': isEditorPage }">
        <slot />
      </main>
    </section>
  </div>
</template>
