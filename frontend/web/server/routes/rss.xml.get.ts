import { escapeXml } from '../utils/xml'

interface Article {
  title: string
  slug: string
  summary: string | null
  publishedAt: string
}

interface Page {
  items: Article[]
}

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const siteUrl = config.public.siteUrl.replace(/\/$/, '')
  const page = await $fetch<Page>('/public/articles', {
    baseURL: config.apiBase,
    query: { page: 1, pageSize: 20 }
  })
  const items = page.items.map((article) => {
    const link = `${siteUrl}/article/${article.slug}`
    return `<item><title>${escapeXml(article.title)}</title><link>${escapeXml(link)}</link><guid isPermaLink="true">${escapeXml(link)}</guid><description>${escapeXml(article.summary)}</description><pubDate>${new Date(article.publishedAt).toUTCString()}</pubDate></item>`
  }).join('')

  setHeader(event, 'content-type', 'application/rss+xml; charset=utf-8')
  return `<?xml version="1.0" encoding="UTF-8"?><rss version="2.0"><channel><title>CageWang‘s Blog</title><link>${escapeXml(siteUrl)}</link><description>记录技术、生活与灵光的个人博客。</description><language>zh-CN</language>${items}</channel></rss>`
})
