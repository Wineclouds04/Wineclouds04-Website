<script setup lang="ts">
const foundations = [
  ['Spring Boot 4.1', '模块化单体 API'],
  ['Nuxt 4', 'SSR 与 SEO 基础'],
  ['MySQL + Redis', '可靠数据与短期状态'],
  ['Docker Compose', '一致的本地运行环境']
]
</script>

<template>
  <main>
    <section class="hero">
      <nav class="nav">
        <a class="brand" href="/">PB<span>.</span></a>
        <span class="phase">Phase 01 · Infrastructure</span>
      </nav>

      <div class="hero-copy">
        <p class="eyebrow">PERSONAL BLOG / 个人博客</p>
        <h1>先把地基<br><em>安静地打牢。</em></h1>
        <p class="intro">
          工程、数据与部署链路已经接通。文章、作品与想法，将从这里开始生长。
        </p>
        <div class="actions">
          <a class="primary" href="/api/v1/status">查看 API 状态</a>
          <a class="secondary" href="http://admin.localhost/">进入管理端</a>
        </div>
      </div>
    </section>

    <section class="foundation">
      <p class="section-label">FOUNDATION / 技术底座</p>
      <div class="grid">
        <article v-for="[title, description] in foundations" :key="title">
          <span class="dot" />
          <h2>{{ title }}</h2>
          <p>{{ description }}</p>
        </article>
      </div>
    </section>
  </main>
</template>

