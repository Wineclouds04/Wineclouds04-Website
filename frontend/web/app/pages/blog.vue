<script setup lang="ts">
const route = useRoute()
const api = useBlogApi()
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const { data } = await useAsyncData(
  () => `articles-${page.value}`,
  () => api.articles({ page: page.value }),
  { watch: [page] }
)

useSeoMeta({
  title: '全部文章 · CageWang‘s Blog',
  description: '浏览 CageWang‘s Blog 的全部公开文章。'
})
</script>

<template>
  <div>
    <PageIntro
      eyebrow="ALL NOTES"
      title="全部文章"
      description="不按重要程度，只按写下它们的时间排列。"
      :count="data?.total"
    />
    <section class="section content-width compact-top">
      <div v-if="data?.items.length" class="article-grid">
        <ArticleCard
          v-for="(article, index) in data.items"
          :key="article.id"
          :article="article"
          :index="(page - 1) * data.pageSize + index"
        />
      </div>
      <EmptyState v-else />
      <PaginationNav
        v-if="data"
        :page="data.page"
        :total-pages="data.totalPages"
      />
    </section>
  </div>
</template>
