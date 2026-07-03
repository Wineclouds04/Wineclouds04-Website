<script setup lang="ts">
const route = useRoute()
const api = useBlogApi()
const slug = computed(() => String(route.params.slug))
const page = computed(() => Math.max(1, Number(route.query.page) || 1))

const [{ data: categories }, { data: articles }] = await Promise.all([
  useAsyncData('categories', () => api.categories()),
  useAsyncData(
    () => `category-${slug.value}-${page.value}`,
    () => api.categoryArticles(slug.value, page.value),
    { watch: [page] }
  )
])
const current = computed(() => categories.value?.find((item) => item.slug === slug.value))

useSeoMeta({
  title: () => `${current.value?.name || '分类'} · CageWang‘s Blog`,
  description: () => current.value?.description || `浏览 ${current.value?.name || ''} 分类下的文章。`
})
</script>

<template>
  <div>
    <PageIntro
      eyebrow="CATEGORY"
      :title="current?.name || '分类'"
      :description="current?.description || '收拢在这个主题下的全部文章。'"
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
