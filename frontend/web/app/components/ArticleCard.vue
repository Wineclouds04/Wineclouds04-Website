<script setup lang="ts">
import type { ArticleCard } from '~/types/blog'

defineProps<{
  article: ArticleCard
  featured?: boolean
  index?: number
}>()
</script>

<template>
  <article class="article-card" :class="{ featured }">
    <NuxtLink
      class="article-card-link"
      :to="`/article/${article.slug}`"
    >
      <div class="card-top">
        <span v-if="article.categoryName" class="card-category">
          <i class="iconfont icon-folder" aria-hidden="true" />
          {{ article.categoryName }}
        </span>
        <time :datetime="article.publishedAt">
          <i class="iconfont icon-time" aria-hidden="true" />
          {{ formatDate(article.publishedAt) }}
        </time>
        <span v-if="article.pinned" class="pinned">置顶</span>
      </div>
      <div class="card-body">
        <h2>{{ article.title }}</h2>
        <p>{{ article.summary || '一篇还没来得及写摘要的文章，正文里见。' }}</p>
      </div>
      <div class="card-meta">
        <span>
          <i class="iconfont icon-eye" aria-hidden="true" />
          {{ article.viewCount }}
        </span>
        <span>
          <i class="iconfont icon-guidang" aria-hidden="true" />
          第 {{ String((index ?? 0) + 1).padStart(2, '0') }} 篇
        </span>
        <span>
          <i class="iconfont icon-time" aria-hidden="true" />
          {{ article.readingMinutes }} 分钟
        </span>
      </div>
    </NuxtLink>
  </article>
</template>
