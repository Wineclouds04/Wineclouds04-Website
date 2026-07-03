<script setup lang="ts">
const api = useBlogApi()
const { data } = await useAsyncData('tags', () => api.tags())

useSeoMeta({
  title: '文章标签 · CageWang‘s Blog',
  description: '从标签出发，发现相互关联的文章。'
})
</script>

<template>
  <div>
    <PageIntro eyebrow="TAGS" title="文章标签" description="一些比分类更轻、更自由的线索。" />
    <section class="section content-width compact-top">
      <div v-if="data?.length" class="tag-wall">
        <NuxtLink v-for="item in data" :key="item.id" :to="`/tag/${item.slug}`">
          <span>#</span>
          <strong>{{ item.name }}</strong>
          <small>{{ item.articleCount }} 篇文章</small>
        </NuxtLink>
      </div>
      <EmptyState v-else />
    </section>
  </div>
</template>
