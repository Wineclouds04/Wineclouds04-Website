import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { guest: true }
    },
    {
      path: '/',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/articles',
      name: 'articles',
      component: () => import('../views/ArticleListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/articles/new',
      name: 'article-new',
      component: () => import('../views/ArticleEditorView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/articles/:id(\\d+)',
      name: 'article-edit',
      component: () => import('../views/ArticleEditorView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/categories',
      name: 'categories',
      component: () => import('../views/TaxonomyView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/tags',
      name: 'tags',
      component: () => import('../views/TaxonomyView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/media',
      name: 'media',
      component: () => import('../views/MediaView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/comments',
      name: 'comments',
      component: () => import('../views/CommentModerationView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/operation-logs',
      name: 'operation-logs',
      component: () => import('../views/OperationLogView.vue'),
      meta: { requiresAuth: true }
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

export default router
