<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { ApiError } from '@personal-blog/api-client'

import { api } from '../services/api'
import { useAuthStore } from '../stores/auth'

type TaxonomyItem = {
  id: number
  name: string
  slug: string
  description?: string
  sortOrder: number
  visible: boolean
  articleCount: number
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const kind = computed(() => route.name === 'tags' ? 'tags' : 'categories')
const label = computed(() => kind.value === 'tags' ? '标签' : '分类')
const canWrite = computed(() => auth.user?.role === 'ADMIN')
const items = ref<TaxonomyItem[]>([])
const editingId = ref<number | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const form = reactive({
  name: '',
  slug: '',
  description: '',
  sortOrder: 0,
  visible: true
})

const reset = () => {
  editingId.value = null
  Object.assign(form, {
    name: '',
    slug: '',
    description: '',
    sortOrder: 0,
    visible: true
  })
}

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    items.value = await api.get<TaxonomyItem[]>(`/admin/${kind.value}`)
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : `${label.value}加载失败`
  } finally {
    loading.value = false
  }
}

const edit = (item: TaxonomyItem) => {
  editingId.value = item.id
  Object.assign(form, {
    name: item.name,
    slug: item.slug,
    description: item.description ?? '',
    sortOrder: item.sortOrder,
    visible: item.visible
  })
}

const save = async () => {
  if (!canWrite.value || saving.value) return
  saving.value = true
  error.value = ''
  try {
    if (editingId.value === null) {
      await api.post(`/admin/${kind.value}`, form)
    } else {
      await api.put(`/admin/${kind.value}/${editingId.value}`, form)
    }
    reset()
    await load()
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : `${label.value}保存失败`
  } finally {
    saving.value = false
  }
}

const remove = async (item: TaxonomyItem) => {
  if (!canWrite.value || !window.confirm(`确认删除${label.value}“${item.name}”？`)) return
  try {
    await api.delete(`/admin/${kind.value}/${item.id}`)
    if (editingId.value === item.id) reset()
    await load()
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : `${label.value}删除失败`
  }
}

watch(kind, () => {
  reset()
  void load()
})
onMounted(load)
</script>

<template>
  <div class="content-page taxonomy-page">
    <header class="content-header">
      <div>
        <RouterLink class="back-link" to="/">← 返回总览</RouterLink>
        <p class="kicker">CONTENT / TAXONOMY</p>
        <h1>{{ label }}管理</h1>
      </div>
      <nav class="taxonomy-tabs">
        <button
          type="button"
          :class="{ active: kind === 'categories' }"
          @click="router.push('/categories')"
        >
          分类
        </button>
        <button
          type="button"
          :class="{ active: kind === 'tags' }"
          @click="router.push('/tags')"
        >
          标签
        </button>
      </nav>
    </header>

    <p v-if="error" class="page-error">{{ error }}</p>

    <div class="taxonomy-layout">
      <section class="taxonomy-list" :class="{ loading }">
        <div v-if="!loading && items.length === 0" class="empty-state">
          <b>还没有{{ label }}。</b>
          <span>在右侧创建第一个{{ label }}。</span>
        </div>
        <article v-for="item in items" :key="item.id">
          <div>
            <strong>{{ item.name }}</strong>
            <small>/{{ item.slug }}</small>
            <p>{{ item.description || '暂无描述' }}</p>
          </div>
          <div class="taxonomy-meta">
            <span>{{ item.articleCount }} 篇文章</span>
            <span>{{ item.visible ? '公开' : '隐藏' }}</span>
            <button v-if="canWrite" type="button" @click="edit(item)">编辑</button>
            <button v-if="canWrite" class="danger" type="button" @click="remove(item)">
              删除
            </button>
          </div>
        </article>
      </section>

      <form v-if="canWrite" class="taxonomy-form" @submit.prevent="save">
        <p class="kicker">{{ editingId === null ? 'CREATE' : 'UPDATE' }}</p>
        <h2>{{ editingId === null ? `新建${label}` : `编辑${label}` }}</h2>
        <label>
          <span>名称</span>
          <input v-model="form.name" required maxlength="64">
        </label>
        <label>
          <span>SLUG</span>
          <input
            v-model="form.slug"
            required
            maxlength="80"
            pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
            placeholder="backend-notes"
          >
        </label>
        <label>
          <span>描述</span>
          <textarea v-model="form.description" rows="4" maxlength="300" />
        </label>
        <label>
          <span>排序</span>
          <input v-model.number="form.sortOrder" type="number" min="0">
        </label>
        <label class="check-row">
          <input v-model="form.visible" type="checkbox">
          <span>在公开站显示</span>
        </label>
        <div class="taxonomy-form-actions">
          <button v-if="editingId !== null" type="button" @click="reset">取消</button>
          <button class="primary-action" type="submit" :disabled="saving">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
