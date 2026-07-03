<script setup lang="ts">
defineProps<{ title?: string; description?: string }>()
</script>

<template>
  <div class="empty-state">
    <span class="empty-state-icon" aria-hidden="true">
      <i class="iconfont icon-boke" />
    </span>
    <h2>{{ title || '这里暂时还是一片余白' }}</h2>
    <p>{{ description || '内容正在路上，过一阵再来看看吧。' }}</p>
  </div>
</template>
