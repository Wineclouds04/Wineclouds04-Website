export default defineNuxtConfig({
  compatibilityDate: '2026-07-01',
  devtools: { enabled: false },
  css: ['~/assets/css/main.css'],
  runtimeConfig: {
    public: {
      apiBase: '/api/v1'
    }
  },
  app: {
    head: {
      htmlAttrs: { lang: 'zh-CN' },
      title: 'Personal Blog',
      meta: [
        {
          name: 'description',
          content: '记录技术、创作与生活的个人博客。'
        }
      ]
    }
  },
  nitro: {
    preset: 'node-server'
  }
})

