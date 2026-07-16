import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { guest: true, title: '登录' }
    },
    {
      path: '/',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
      meta: { requiresAuth: true, title: '仪表盘' }
    },
    {
      path: '/articles',
      name: 'articles',
      component: () => import('../views/ArticleListView.vue'),
      meta: { requiresAuth: true, title: '文章管理' }
    },
    {
      path: '/articles/new',
      name: 'article-new',
      component: () => import('../views/ArticleEditorView.vue'),
      meta: { requiresAuth: true, title: '新建文章' }
    },
    {
      path: '/articles/:id(\\d+)',
      name: 'article-edit',
      component: () => import('../views/ArticleEditorView.vue'),
      meta: { requiresAuth: true, title: '编辑文章' }
    },
    {
      path: '/categories',
      name: 'categories',
      component: () => import('../views/TaxonomyView.vue'),
      meta: { requiresAuth: true, title: '分类管理' }
    },
    {
      path: '/tags',
      name: 'tags',
      component: () => import('../views/TaxonomyView.vue'),
      meta: { requiresAuth: true, title: '标签管理' }
    },
    {
      path: '/media',
      name: 'media',
      component: () => import('../views/MediaView.vue'),
      meta: { requiresAuth: true, title: '媒体管理' }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/SiteProfileView.vue'),
      meta: { requiresAuth: true, title: '站点资料' }
    },
    {
      path: '/statistics',
      name: 'statistics',
      component: () => import('../views/SiteStatisticsView.vue'),
      meta: { requiresAuth: true, title: '站点统计' }
    },
    {
      path: '/comments',
      name: 'comments',
      component: () => import('../views/CommentModerationView.vue'),
      meta: { requiresAuth: true, title: '评论管理' }
    },
    {
      path: '/operation-logs',
      name: 'operation-logs',
      component: () => import('../views/OperationLogView.vue'),
      meta: { requiresAuth: true, title: '操作日志' }
    }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const signedIn = await auth.restore()

  if (to.meta.requiresAuth && !signedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && signedIn) return { name: 'dashboard' }
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? '管理')} - Wineclouds’Blog Admin`
})

export default router
