<script setup lang="ts">
const props = defineProps<{
  page: number
  totalPages: number
}>()

const route = useRoute()
const target = (page: number) => ({
  path: route.path,
  query: { ...route.query, page: page > 1 ? page : undefined }
})
</script>

<template>
  <nav v-if="totalPages > 1" class="pagination" aria-label="分页">
    <NuxtLink v-if="page > 1" :to="target(page - 1)">
      <i class="iconfont icon-arrow-left" aria-hidden="true" />
      上一页
    </NuxtLink>
    <span>第 {{ page }} / {{ totalPages }} 页</span>
    <NuxtLink v-if="page < totalPages" :to="target(page + 1)">
      下一页
      <i class="iconfont icon-arrow-right" aria-hidden="true" />
    </NuxtLink>
  </nav>
</template>
