<script setup lang="ts">
useSeoMeta({
  title: '隐私说明 · CageWang‘s Blog',
  description: '了解 CageWang‘s Blog 如何使用匿名标识、保存互动信息并保护访客隐私。'
})
</script>

<template>
  <div>
    <PageIntro
      eyebrow="PRIVACY"
      title="少收集，认真保管"
      description="这里尽量只保留网站正常运行所需的信息，不使用隐蔽指纹，也不出售或交换访客数据。"
    />
    <article class="privacy-page reading-width">
      <section>
        <span>01</span>
        <div>
          <h2>匿名访问标识</h2>
          <p>首次互动时会生成一个随机匿名标识，用于防止重复点赞、限制滥用和近似统计独立访客。数据库只保存其 HMAC 摘要，不采集 Canvas、字体或设备指纹。</p>
        </div>
      </section>
      <section>
        <span>02</span>
        <div>
          <h2>评论</h2>
          <p>昵称和正文会在审核通过后公开。邮箱仅用于回复通知，采用加密存储，后台也只显示脱敏结果。原始 IP 不会长期保存。</p>
        </div>
      </section>
      <section>
        <span>03</span>
        <div>
          <h2>访问统计</h2>
          <p>访问量先在 Redis 中匿名聚合，再按天写入统计表。日志中的客户端摘要使用每日轮换材料生成，不能用于跨日期追踪个人。</p>
        </div>
      </section>
      <section>
        <span>04</span>
        <div>
          <h2>数据删除</h2>
          <p>如需删除自己提交的评论或关联邮箱，请通过站点公开联系方式说明内容和大致提交时间，以便核验与处理。</p>
        </div>
      </section>
    </article>
  </div>
</template>
