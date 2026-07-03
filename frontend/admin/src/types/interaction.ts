export interface OperationLogItem {
  id: number
  operatorId: number | null
  operatorName: string | null
  module: string
  action: string
  targetId: string | null
  result: string
  detailJson: string
  traceId: string | null
  createdAt: string
}

interface DashboardTrendPoint {
  date: string
  views: number
  visitors: number
}

interface DashboardPopularArticle {
  id: number
  title: string
  slug: string
  views: number
  likes: number
  comments: number
}

export interface DashboardResponse {
  articles: number
  publishedArticles: number
  comments: number
  pendingComments: number
  messages: number
  likes: number
  views30d: number
  visitors30d: number
  trend: DashboardTrendPoint[]
  popularArticles: DashboardPopularArticle[]
  recentOperations: OperationLogItem[]
  pendingNotifications: number
  mailConfigured: boolean
  serviceStatus: string
  cacheHits: number
  cacheMisses: number
  cacheErrors: number
  cacheHitRate: number
}

export interface AdminCommentItem {
  id: number
  articleId: number | null
  articleTitle: string | null
  parentId: number | null
  type: 'ARTICLE' | 'MESSAGE'
  contentMarkdown: string
  nickname: string
  emailMasked: string | null
  website: string | null
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SPAM' | 'HIDDEN'
  adminReply: boolean
  ipSummary: string
  createdAt: string
}

export interface AdminCommentPage {
  items: AdminCommentItem[]
  total: number
  page: number
  pageSize: number
}

export interface OperationLogPage {
  items: OperationLogItem[]
  total: number
  page: number
  pageSize: number
}
