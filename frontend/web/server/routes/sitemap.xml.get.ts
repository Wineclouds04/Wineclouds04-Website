import { escapeXml } from '../utils/xml'

interface Article {
  slug: string
  updatedAt: string
}

interface Page {
  items: Article[]
  totalPages: number
}

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const siteUrl = config.public.siteUrl.replace(/\/$/, '')
  const first = await $fetch<Page>('/public/articles', {
    baseURL: config.apiBase,
    query: { page: 1, pageSize: 50 }
  })
  const pages = await Promise.all(
    Array.from({ length: Math.max(0, first.totalPages - 1) }, (_, index) =>
      $fetch<Page>('/public/articles', {
        baseURL: config.apiBase,
        query: { page: index + 2, pageSize: 50 }
      })
    )
  )
  const articles = [first, ...pages].flatMap((page) => page.items)
  const staticPaths = ['/', '/blog', '/category', '/tag', '/archive', '/privacy']
  const urls = [
    ...staticPaths.map((path) => `<url><loc>${escapeXml(siteUrl + path)}</loc></url>`),
    ...articles.map((article) =>
      `<url><loc>${escapeXml(`${siteUrl}/article/${article.slug}`)}</loc><lastmod>${escapeXml(article.updatedAt)}</lastmod></url>`
    )
  ].join('')

  setHeader(event, 'content-type', 'application/xml; charset=utf-8')
  return `<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">${urls}</urlset>`
})
