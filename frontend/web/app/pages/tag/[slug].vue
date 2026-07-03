<script setup lang="ts">
const route = useRoute()
const api = useBlogApi()
const slug = computed(() => String(route.params.slug))
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const [{ data: tags }, { data: articles }] = await Promise.all([
  useAsyncData('tags', () => api.tags()),
  useAsyncData(
    () => `tag-${slug.value}-${page.value}`,
    () => api.tagArticles(slug.value, page.value),
    { watch: [page] }
  )
])
const current = computed(() => tags.value?.find((item) => item.slug === slug.value))

useSeoMeta({
  title: () => `#${current.value?.name || '标签'} · CageWang‘s Blog`,
  description: () => current.value?.description || `浏览带有 ${current.value?.name || ''} 标签的文章。`
})
</script>

<template>
  <div>
    <PageIntro
      eyebrow="TAG"
      :title="`# ${current?.name || '标签'}`"
      :description="current?.description || '沿着这个标签，看看思绪会通向哪里。'"
      :count="articles?.total"
    />
    <section class="section content-width compact-top">
      <div v-if="articles?.items.length" class="article-grid">
        <ArticleCard
          v-for="(article, index) in articles.items"
          :key="article.id"
          :article="article"
          :index="index"
        />
      </div>
      <EmptyState v-else />
      <PaginationNav
        v-if="articles"
        :page="articles.page"
        :total-pages="articles.totalPages"
      />
    </section>
  </div>
</template>
