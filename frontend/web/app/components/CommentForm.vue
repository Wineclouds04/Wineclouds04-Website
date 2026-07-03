<script setup lang="ts">
import type { CommentInput } from '~/types/blog'

const props = withDefaults(defineProps<{
  articleId: number
  parentId?: number | null
  compact?: boolean
}>(), {
  parentId: null,
  compact: false
})

const emit = defineEmits<{ submitted: [] }>()
const api = useBlogApi()
const form = reactive<CommentInput>({
  nickname: '',
  email: '',
  website: '',
  content: '',
  parentId: props.parentId,
  notifyOnReply: true,
  captchaId: '',
  captchaAnswer: ''
})
const submitting = ref(false)
const captchaLoading = ref(false)
const captchaQuestion = ref('')
const error = ref('')
const success = ref('')

const loadCaptcha = async () => {
  captchaLoading.value = true
  try {
    const challenge = await api.captcha()
    form.captchaId = challenge.id
    form.captchaAnswer = ''
    captchaQuestion.value = challenge.question
  } catch {
    captchaQuestion.value = '安全验证暂时不可用'
  } finally {
    captchaLoading.value = false
  }
}

onMounted(loadCaptcha)

const submit = async () => {
  if (submitting.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const result = await api.submitComment(props.articleId, form)
    success.value = result.message
    form.content = ''
    await loadCaptcha()
    emit('submitted')
  } catch (cause: any) {
    error.value = cause?.data?.detail || cause?.message || '提交失败，请稍后重试'
    await loadCaptcha()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="comment-form" :class="{ compact }" @submit.prevent="submit">
    <div class="comment-fields">
      <label>
        <span>昵称 *</span>
        <input v-model.trim="form.nickname" required minlength="2" maxlength="64" autocomplete="name">
      </label>
      <label>
        <span>邮箱</span>
        <input v-model.trim="form.email" type="email" maxlength="160" autocomplete="email">
      </label>
      <label v-if="!compact">
        <span>个人网站</span>
        <input v-model.trim="form.website" type="url" maxlength="300" placeholder="https://">
      </label>
    </div>
    <label class="comment-content">
      <span>{{ compact ? '写下回复 *' : '写下想说的话 *' }}</span>
      <textarea
        v-model.trim="form.content"
        required
        minlength="2"
        maxlength="2000"
        :rows="compact ? 4 : 6"
        placeholder="支持 Markdown；审核通过后公开显示。"
      />
    </label>
    <div class="captcha-row">
      <label>
        <span>安全验证 *</span>
        <input
          v-model.trim="form.captchaAnswer"
          required
          maxlength="16"
          inputmode="numeric"
          :placeholder="captchaQuestion"
          :disabled="captchaLoading || !form.captchaId"
        >
      </label>
      <button type="button" :disabled="captchaLoading" @click="loadCaptcha">
        {{ captchaLoading ? '生成中…' : '换一道' }}
      </button>
    </div>
    <div class="comment-submit">
      <label class="notify-choice">
        <input v-model="form.notifyOnReply" type="checkbox" :disabled="!form.email">
        <span>有新回复时邮件通知我</span>
      </label>
      <button class="button" type="submit" :disabled="submitting">
        {{ submitting ? '正在投递…' : compact ? '提交回复' : '提交审核' }}
      </button>
    </div>
    <p v-if="error" class="form-message error">{{ error }}</p>
    <p v-if="success" class="form-message success">{{ success }}</p>
  </form>
</template>
