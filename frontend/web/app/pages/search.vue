<script setup lang="ts">
const route = useRoute()
const router = useRouter()
const api = useBlogApi()
const keyword = computed(() => String(route.query.q || '').trim())
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const draft = ref(keyword.value)
const { data, status } = await useAsyncData(
  () => `search-${keyword.value}-${page.value}`,
  () => keyword.value ? api.search(keyword.value, page.value) : Promise.resolve(null),
  { watch: [keyword, page] }
)

const submit = () => {
  router.push({ path: '/search', query: { q: draft.value.trim() || undefined } })
}

useSeoMeta({
  title: () => keyword.value ? `搜索“${keyword.value}” · CageWang‘s Blog` : '搜索 · CageWang‘s Blog',
  robots: 'noindex,follow'
})
</script>

<template>
  <div>
    <PageIntro eyebrow="SEARCH" title="搜索文章" description="输入一个词，看看它曾在哪些段落里出现。" />
    <section class="search-section content-width">
      <form class="search-box" @submit.prevent="submit">
        <input v-model="draft" type="search" maxlength="100" placeholder="技术、生活、灵感……" aria-label="搜索关键词">
        <button class="button primary" type="submit">开始寻找</button>
      </form>
      <p v-if="keyword && data" class="search-summary">
        “{{ keyword }}” 找到 {{ data.total }} 个结果
      </p>
      <div v-if="status === 'pending'" class="loading-line">正在翻找札记……</div>
      <div v-else-if="data?.items.length" class="article-grid">
        <ArticleCard
          v-for="(article, index) in data.items"
          :key="article.id"
          :article="article"
          :index="index"
        />
      </div>
      <EmptyState
        v-else-if="keyword"
        title="没有找到相符的文章"
        description="换个更短的关键词，也许会有新的线索。"
      />
      <PaginationNav
        v-if="data"
        :page="data.page"
        :total-pages="data.totalPages"
      />
    </section>
  </div>
</template>
