<script setup lang="ts">
const api = useBlogApi()
const { data } = await useAsyncData('categories', () => api.categories())

useSeoMeta({
  title: '文章分类 · CageWang‘s Blog',
  description: '按主题浏览 CageWang‘s Blog。'
})
</script>

<template>
  <div>
    <PageIntro eyebrow="CATEGORIES" title="文章分类" description="把相近的思考，放在同一层书架上。" />
    <section class="section content-width compact-top">
      <div v-if="data?.length" class="taxonomy-list">
        <NuxtLink v-for="(item, index) in data" :key="item.id" :to="`/category/${item.slug}`">
          <span class="taxonomy-index">{{ String(index + 1).padStart(2, '0') }}</span>
          <div>
            <h2>{{ item.name }}</h2>
            <p>{{ item.description || '这个分类还没有写简介。' }}</p>
          </div>
          <span class="taxonomy-count">{{ item.articleCount }} 篇</span>
          <i class="taxonomy-arrow iconfont icon-arrow-right" aria-hidden="true" />
        </NuxtLink>
      </div>
      <EmptyState v-else />
    </section>
  </div>
</template>
