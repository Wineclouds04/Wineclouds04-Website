import type {
  ArchiveMonth,
  ArticleDetail,
  ArticleNavigation,
  ArticlePage,
  CaptchaChallenge,
  CommentInput,
  CommentSubmitResponse,
  HomeResponse,
  InteractionState,
  PublicComment,
  TaxonomyItem
} from '~/types/blog'

export function useBlogApi() {
  const config = useRuntimeConfig()
  const baseURL = import.meta.server ? config.apiBase : config.public.apiBase
  const request = <T>(path: string, query?: Record<string, string | number | undefined>) =>
    $fetch<T>(path, { baseURL, query })

  return {
    home: () => request<HomeResponse>('/public/home'),
    articles: (query?: Record<string, string | number | undefined>) =>
      request<ArticlePage>('/public/articles', query),
    article: (slug: string) => request<ArticleDetail>(`/public/articles/${encodeURIComponent(slug)}`),
    adjacent: (slug: string) =>
      request<ArticleNavigation>(`/public/articles/${encodeURIComponent(slug)}/adjacent`),
    related: (slug: string) =>
      request<ArticlePage['items']>(`/public/articles/${encodeURIComponent(slug)}/related`),
    categories: () => request<TaxonomyItem[]>('/public/categories'),
    categoryArticles: (slug: string, page: number) =>
      request<ArticlePage>(`/public/categories/${encodeURIComponent(slug)}/articles`, { page }),
    tags: () => request<TaxonomyItem[]>('/public/tags'),
    tagArticles: (slug: string, page: number) =>
      request<ArticlePage>(`/public/tags/${encodeURIComponent(slug)}/articles`, { page }),
    archives: () => request<ArchiveMonth[]>('/public/archives'),
    search: (keyword: string, page: number) =>
      request<ArticlePage>('/public/search', { keyword, page }),
    captcha: () => request<CaptchaChallenge>('/public/captcha'),
    comments: (articleId: number) =>
      request<PublicComment[]>(`/public/articles/${articleId}/comments`),
    submitComment: (articleId: number, body: CommentInput) =>
      $fetch<CommentSubmitResponse>(`/public/articles/${articleId}/comments`, {
        baseURL,
        method: 'POST',
        body
      }),
    interaction: (articleId: number) =>
      $fetch<InteractionState>(`/public/articles/${articleId}/interaction`, { baseURL }),
    like: (articleId: number) =>
      $fetch<InteractionState>(`/public/articles/${articleId}/likes`, {
        baseURL,
        method: 'POST'
      }),
    unlike: (articleId: number) =>
      $fetch<InteractionState>(`/public/articles/${articleId}/likes`, {
        baseURL,
        method: 'DELETE'
      }),
    recordView: (articleId: number) =>
      $fetch<{ accepted: boolean }>(`/public/articles/${articleId}/views`, {
        baseURL,
        method: 'POST'
      })
  }
}
