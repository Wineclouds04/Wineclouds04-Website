export default defineNuxtConfig({
  compatibilityDate: '2026-07-01',
  devtools: { enabled: false },
  css: ['~/assets/css/main.css'],
  runtimeConfig: {
    apiBase: process.env.NUXT_API_BASE || 'http://localhost/api/v1',
    public: {
      apiBase: '/api/v1',
      siteUrl: process.env.NUXT_PUBLIC_SITE_URL || 'http://localhost'
    }
  },
  app: {
    head: {
      htmlAttrs: { lang: 'zh-CN' },
      title: 'CageWang‘s Blog',
      meta: [
        {
          name: 'description',
          content: '记录技术、创作与生活的个人博客。'
        },
        { name: 'theme-color', content: '#f4f1e9' }
      ],
      link: [{ rel: 'alternate', type: 'application/rss+xml', title: 'CageWang‘s Blog RSS', href: '/rss.xml' }],
      script: [{
        innerHTML: "try{var t=localStorage.getItem('blog-theme');if(t==='dark'||(!t&&matchMedia('(prefers-color-scheme: dark)').matches))document.documentElement.dataset.theme='dark'}catch(e){}"
      }]
    }
  },
  nitro: {
    preset: 'node-server',
    devProxy: {
      '/api': {
        target: 'http://127.0.0.1/api',
        changeOrigin: true
      }
    },
    routeRules: process.env.NODE_ENV === 'development'
      ? {}
      : {
          '/': { swr: 120 },
          '/article/**': { swr: 300 },
          '/category/**': { swr: 120 },
          '/tag/**': { swr: 120 },
          '/archive': { swr: 300 },
          '/sitemap.xml': { swr: 3600 },
          '/rss.xml': { swr: 900 }
        }
  }
})
