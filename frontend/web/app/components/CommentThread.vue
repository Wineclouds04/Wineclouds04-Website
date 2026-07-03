<script setup lang="ts">
import type { PublicComment } from '~/types/blog'

defineProps<{
  comments: PublicComment[]
  articleId: number
}>()

const replyingTo = ref<number | null>(null)

const toggleReply = (id: number) => {
  replyingTo.value = replyingTo.value === id ? null : id
}
</script>

<template>
  <div v-if="comments.length" class="comment-list">
    <article
      v-for="comment in comments"
      :id="`comment-${comment.id}`"
      :key="comment.id"
      class="comment-item"
    >
      <header>
        <div class="comment-avatar">{{ comment.adminReply ? 'C' : comment.nickname.slice(0, 1) }}</div>
        <div>
          <strong>{{ comment.nickname }}</strong>
          <span v-if="comment.adminReply">博主</span>
          <time :datetime="comment.createdAt">{{ formatDate(comment.createdAt) }}</time>
        </div>
      </header>
      <div class="comment-body" v-html="comment.contentHtml" />
      <button class="text-action" type="button" @click="toggleReply(comment.id)">
        {{ replyingTo === comment.id ? '收起回复' : '回复' }}
      </button>
      <CommentForm
        v-if="replyingTo === comment.id"
        :article-id="articleId"
        :parent-id="comment.id"
        compact
        @submitted="replyingTo = null"
      />

      <div v-if="comment.replies.length" class="comment-replies">
        <article
          v-for="reply in comment.replies"
          :id="`comment-${reply.id}`"
          :key="reply.id"
          class="comment-item reply"
        >
          <header>
            <div class="comment-avatar">{{ reply.adminReply ? 'C' : reply.nickname.slice(0, 1) }}</div>
            <div>
              <strong>{{ reply.nickname }}</strong>
              <span v-if="reply.adminReply">博主</span>
              <time :datetime="reply.createdAt">{{ formatDate(reply.createdAt) }}</time>
            </div>
          </header>
          <div class="comment-body" v-html="reply.contentHtml" />
        </article>
      </div>
    </article>
  </div>
  <div v-else class="comment-empty">还没有公开回应。你可以成为第一个写下文字的人。</div>
</template>
