export type ArticleStatus = 'DRAFT' | 'SCHEDULED' | 'PUBLISHED' | 'ARCHIVED'
type ArticleVisibility = 'PUBLIC' | 'PRIVATE'

export type ArticleListItem = {
  id: number
  title: string
  slug: string
  summary?: string
  status: ArticleStatus
  visibility: ArticleVisibility
  pinned: boolean
  categoryName?: string
  wordCount: number
  viewCount: number
  publishedAt?: string
  updatedAt: string
  version: number
}

export type ArticlePage = {
  items: ArticleListItem[]
  total: number
  page: number
  pageSize: number
}

export type ArticleDetail = {
  id: number
  title: string
  slug: string
  summary?: string
  contentMarkdown: string
  contentHtml: string
  categoryId?: number
  tagIds: number[]
  status: ArticleStatus
  visibility: ArticleVisibility
  pinned: boolean
  allowComment: boolean
  wordCount: number
  readingMinutes: number
  viewCount: number
  metaTitle?: string
  metaDescription?: string
  canonicalUrl?: string
  publishedAt?: string
  createdAt: string
  updatedAt: string
  version: number
}

export type ArticleInput = {
  title: string
  slug: string
  summary: string
  contentMarkdown: string
  categoryId: number | null
  tagIds: number[]
  visibility: ArticleVisibility
  pinned: boolean
  allowComment: boolean
  metaTitle: string
  metaDescription: string
  canonicalUrl: string
  version: number
}

export type TaxonomyOption = {
  id: number
  name: string
  slug: string
}
