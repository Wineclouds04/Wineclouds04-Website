export interface ArticleCard {
  id: number
  title: string
  slug: string
  summary: string | null
  categoryName: string | null
  categorySlug: string | null
  pinned: boolean
  readingMinutes: number
  viewCount: number
  publishedAt: string
  updatedAt: string
}

export interface TaxonomyItem {
  id: number
  name: string
  slug: string
  description: string | null
  articleCount: number
}

export interface ArticlePage {
  items: ArticleCard[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface ArticleDetail {
  id: number
  title: string
  slug: string
  summary: string | null
  contentHtml: string
  categoryName: string | null
  categorySlug: string | null
  tags: TaxonomyItem[]
  pinned: boolean
  allowComment: boolean
  wordCount: number
  readingMinutes: number
  viewCount: number
  metaTitle: string | null
  metaDescription: string | null
  canonicalUrl: string | null
  publishedAt: string
  updatedAt: string
}

export interface HomeResponse {
  featured: ArticleCard[]
  latest: ArticleCard[]
  categories: TaxonomyItem[]
  tags: TaxonomyItem[]
  articleCount: number
}

export interface SiteProfile {
  avatarUrl: string
  signature: string
}

export interface SiteStatistics {
  onlineVisitors: number
  todayViews: number
  totalViews: number
  totalVisitors: number
}

export interface ArchiveMonth {
  year: number
  month: number
  articles: ArticleCard[]
}

export interface ArticleNavigation {
  previous: ArticleCard | null
  next: ArticleCard | null
}

export interface PublicComment {
  id: number
  parentId: number | null
  nickname: string
  contentHtml: string
  adminReply: boolean
  createdAt: string
  replies: PublicComment[]
}

export interface CommentInput {
  nickname: string
  email: string
  website: string
  content: string
  parentId: number | null
  notifyOnReply: boolean
  captchaId: string
  captchaAnswer: string
}

export interface CommentSubmitResponse {
  id: number
  status: 'PENDING'
  message: string
}

export interface InteractionState {
  likeCount: number
  commentCount: number
  liked: boolean
}

export interface CaptchaChallenge {
  id: string
  question: string
  expiresAt: string
}
