<script setup lang="ts">
const api = useBlogApi()
const { data } = await useAsyncData('archives', () => api.archives())
const count = computed(() => data.value?.reduce((total, group) => total + group.articles.length, 0) || 0)

useSeoMeta({
  title: '文章归档 · CageWang‘s Blog',
  description: '沿着时间线浏览所有公开文章。'
})
</script>

<template>
  <div>
    <PageIntro
      eyebrow="ARCHIVE"
      title="时间归档"
      description="回头看，文字替我们记住了时间的纹理。"
      :count="count"
    />
    <section class="section content-width compact-top">
      <div v-if="data?.length" class="archive-list">
        <section v-for="group in data" :key="`${group.year}-${group.month}`" class="archive-group">
          <div class="archive-date">
            <strong>{{ group.year }}</strong>
            <span>{{ String(group.month).padStart(2, '0') }} 月</span>
          </div>
          <div class="archive-articles">
            <NuxtLink
              v-for="article in group.articles"
              :key="article.id"
              :to="`/article/${article.slug}`"
            >
              <time>{{ formatShortDate(article.publishedAt) }}</time>
              <span>{{ article.title }}</span>
              <small>{{ article.readingMinutes }} min</small>
              <i class="iconfont icon-arrow-right" aria-hidden="true" />
            </NuxtLink>
          </div>
        </section>
      </div>
      <EmptyState v-else />
    </section>
  </div>
</template>
