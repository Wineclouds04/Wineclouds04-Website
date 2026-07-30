<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

import { ApiError } from '@personal-blog/api-client'

import { api } from '../services/api'
import { useAuthStore } from '../stores/auth'
import type { MediaAsset } from '../types/media'

type SiteProfile = {
  avatarUrl: string
  signature: string
  musicEnabled: boolean
  musicTitle: string
  musicArtist: string
  musicUrl: string
  musicCoverUrl: string
  musicPlaylist: MusicTrack[]
  musicVolume: number
}

type MusicTrack = {
  title: string
  artist: string
  url: string
  coverUrl: string
}

const defaultAvatarUrl = '/images/wineclouds-avatar.webp'
const auth = useAuthStore()
const canWrite = computed(() => auth.user?.role === 'ADMIN')
const form = reactive<SiteProfile>({
  avatarUrl: '',
  signature: '',
  musicEnabled: false,
  musicTitle: '',
  musicArtist: '',
  musicUrl: '',
  musicCoverUrl: '',
  musicPlaylist: [],
  musicVolume: 100
})
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const uploadingMusic = ref(false)
const error = ref('')
const success = ref('')
const mediaConfigured = ref(false)
const maxImageSize = ref(10 * 1024 * 1024)
const maxAudioSize = ref(10 * 1024 * 1024)
const fileInput = ref<HTMLInputElement | null>(null)
const musicFileInput = ref<HTMLInputElement | null>(null)
const previewAvatarUrl = computed(() => form.avatarUrl.trim() || defaultAvatarUrl)
const avatarPreviewError = ref(false)

watch(previewAvatarUrl, () => {
  avatarPreviewError.value = false
})

const legacyMusicTrack = (profile: SiteProfile): MusicTrack[] => {
  if (!profile.musicUrl) return []
  return [{
    title: profile.musicTitle,
    artist: profile.musicArtist,
    url: profile.musicUrl,
    coverUrl: profile.musicCoverUrl
  }]
}

const applyProfile = (profile: SiteProfile) => {
  const playlist = profile.musicPlaylist?.length ? profile.musicPlaylist : legacyMusicTrack(profile)
  form.avatarUrl = profile.avatarUrl
  form.signature = profile.signature
  form.musicEnabled = profile.musicEnabled
  form.musicTitle = profile.musicTitle
  form.musicArtist = profile.musicArtist
  form.musicUrl = profile.musicUrl
  form.musicCoverUrl = profile.musicCoverUrl
  form.musicPlaylist = playlist.map((track) => ({ ...track }))
  form.musicVolume = typeof profile.musicVolume === 'number' ? profile.musicVolume : 100
}

const firstTrackPayload = () => form.musicPlaylist[0] ?? {
  title: form.musicTitle,
  artist: form.musicArtist,
  url: form.musicUrl,
  coverUrl: form.musicCoverUrl
}

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const [profile, mediaConfig] = await Promise.all([
      api.get<SiteProfile>('/admin/profile'),
      api.get<{ configured: boolean, maxImageSize: number, maxAudioSize: number }>('/admin/media/config')
    ])
    applyProfile(profile)
    mediaConfigured.value = mediaConfig.configured
    maxImageSize.value = mediaConfig.maxImageSize
    maxAudioSize.value = mediaConfig.maxAudioSize
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '站点资料加载失败'
  } finally {
    loading.value = false
  }
}

const save = async () => {
  if (!canWrite.value || saving.value) return
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const primaryTrack = firstTrackPayload()
    const profile = await api.put<SiteProfile>('/admin/profile', {
      avatarUrl: form.avatarUrl,
      signature: form.signature,
      musicEnabled: form.musicEnabled,
      musicTitle: primaryTrack.title,
      musicArtist: primaryTrack.artist,
      musicUrl: primaryTrack.url,
      musicCoverUrl: primaryTrack.coverUrl,
      musicPlaylist: form.musicPlaylist,
      musicVolume: form.musicVolume
    })
    applyProfile(profile)
    success.value = '站点资料已保存；切换回公开站时会自动刷新资料。'
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '站点资料保存失败'
  } finally {
    saving.value = false
  }
}

const chooseAvatar = () => fileInput.value?.click()

const uploadAvatar = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || uploading.value || !canWrite.value) return
  if (file.size > maxImageSize.value) {
    error.value = `头像图片不能超过 ${Math.round(maxImageSize.value / 1024 / 1024)} MB`
    return
  }
  const body = new FormData()
  body.append('file', file)
  body.append('altText', '站点头像')
  uploading.value = true
  error.value = ''
  success.value = ''
  try {
    const asset = await api.postForm<MediaAsset>('/admin/media', body)
    form.avatarUrl = asset.url
    success.value = '头像已上传。确认预览后点击“保存资料”即可发布。'
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '头像上传失败'
  } finally {
    uploading.value = false
  }
}

const markAvatarPreviewUnavailable = () => {
  avatarPreviewError.value = true
}

const chooseMusic = () => musicFileInput.value?.click()

const addMusicTrack = () => {
  if (!canWrite.value) return
  form.musicPlaylist.push({
    title: '',
    artist: '',
    url: '',
    coverUrl: ''
  })
}

const removeMusicTrack = (index: number) => {
  if (!canWrite.value) return
  form.musicPlaylist.splice(index, 1)
}

const moveMusicTrack = (index: number, offset: -1 | 1) => {
  if (!canWrite.value) return
  const nextIndex = index + offset
  if (nextIndex < 0 || nextIndex >= form.musicPlaylist.length) return
  const [track] = form.musicPlaylist.splice(index, 1)
  form.musicPlaylist.splice(nextIndex, 0, track)
}

const uploadMusic = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || uploadingMusic.value || !canWrite.value) return
  if (file.size > maxAudioSize.value) {
    error.value = `MP3 文件不能超过 ${Math.round(maxAudioSize.value / 1024 / 1024)} MB`
    return
  }

  const body = new FormData()
  body.append('file', file)
  uploadingMusic.value = true
  error.value = ''
  success.value = ''
  try {
    const asset = await api.postForm<MediaAsset>('/admin/media/music', body)
    form.musicEnabled = true
    form.musicPlaylist.push({
      title: file.name.replace(/\.mp3$/i, ''),
      artist: '',
      url: asset.url,
      coverUrl: ''
    })
    success.value = 'MP3 已上传并加入播放列表。调整顺序后点击“保存资料”发布。'
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : 'MP3 上传失败'
  } finally {
    uploadingMusic.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="content-page profile-settings-page">
    <header class="content-header">
      <div>
        <RouterLink class="back-link" to="/">返回总览</RouterLink>
        <p class="kicker">SITE / PROFILE</p>
        <h1>站点资料</h1>
      </div>
    </header>

    <p v-if="error" class="page-error">{{ error }}</p>
    <p v-if="success" class="profile-success">{{ success }}</p>

    <section v-if="!loading" class="profile-settings-layout">
      <aside class="profile-preview-panel">
        <p class="kicker">LIVE PREVIEW</p>
        <img
          v-if="!avatarPreviewError"
          :src="previewAvatarUrl"
          alt="站点头像预览"
          @error="markAvatarPreviewUnavailable"
        >
        <div v-else class="profile-avatar-fallback">头像不可用</div>
        <h2>Wineclouds</h2>
        <p>{{ form.signature || '填写一句签名，展示在首页资料卡中。' }}</p>
      </aside>

      <form class="profile-settings-form" @submit.prevent="save">
        <label>
          <span>头像图片链接</span>
          <input
            v-model.trim="form.avatarUrl"
            :disabled="!canWrite"
            maxlength="1000"
            placeholder="/images/avatar.png 或 https://…"
          >
          <small>支持站内路径或 HTTPS 图片链接。留空时使用默认头像。</small>
        </label>

        <div class="profile-upload-row">
          <input
            ref="fileInput"
            class="visually-hidden"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            @change="uploadAvatar"
          >
          <button
            type="button"
            :disabled="!canWrite || !mediaConfigured || uploading"
            @click="chooseAvatar"
          >
            {{ uploading ? '上传中…' : '上传新头像' }}
          </button>
          <RouterLink to="/media">从媒体库获取链接</RouterLink>
          <small v-if="!mediaConfigured">对象存储未配置时，仍可填写已有图片链接。</small>
        </div>

        <label>
          <span>个人签名</span>
          <textarea
            v-model.trim="form.signature"
            :disabled="!canWrite"
            maxlength="160"
            rows="4"
            required
            placeholder="写一句展示在首页的自我介绍"
          />
          <small>{{ form.signature.length }} / 160</small>
        </label>

        <fieldset class="music-settings">
          <legend>右侧播放器</legend>
          <label class="music-toggle">
            <input v-model="form.musicEnabled" :disabled="!canWrite" type="checkbox">
            <span>在公开站启用播放器</span>
          </label>

          <div v-if="form.musicEnabled" class="music-settings-fields">
            <label class="music-volume-control">
              <span>播放音量 {{ form.musicVolume }}%</span>
              <input
                v-model.number="form.musicVolume"
                :disabled="!canWrite"
                type="range"
                min="0"
                max="100"
                step="1"
              >
            </label>

            <div class="music-playlist-panel">
              <div class="music-playlist-heading">
                <div>
                  <span>播放列表</span>
                  <small>公开站会按列表顺序连续播放；第一首会兼容旧播放器字段。</small>
                </div>
                <button type="button" :disabled="!canWrite" @click="addMusicTrack">添加曲目</button>
              </div>

              <p v-if="!form.musicPlaylist.length" class="music-playlist-empty">还没有曲目，上传 MP3 或手动添加一个链接。</p>

              <div
                v-for="(track, index) in form.musicPlaylist"
                :key="`${track.url}-${index}`"
                class="music-track-item"
              >
                <div class="music-track-order">
                  <strong>{{ index + 1 }}</strong>
                  <button
                    type="button"
                    :disabled="!canWrite || index === 0"
                    :aria-label="`上移第 ${index + 1} 首`"
                    @click="moveMusicTrack(index, -1)"
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    :disabled="!canWrite || index === form.musicPlaylist.length - 1"
                    :aria-label="`下移第 ${index + 1} 首`"
                    @click="moveMusicTrack(index, 1)"
                  >
                    ↓
                  </button>
                </div>

                <div class="music-track-fields">
                  <label>
                    <span>曲目名称</span>
                    <input v-model.trim="track.title" :disabled="!canWrite" maxlength="120" placeholder="夜间播放列表">
                  </label>
                  <label>
                    <span>作者</span>
                    <input v-model.trim="track.artist" :disabled="!canWrite" maxlength="120" placeholder="Wineclouds">
                  </label>
                  <label class="music-track-url">
                    <span>音乐地址</span>
                    <input v-model.trim="track.url" :disabled="!canWrite" maxlength="1000" placeholder="https://cdn.example.com/track.mp3">
                  </label>
                  <label class="music-track-cover">
                    <span>封面地址</span>
                    <input v-model.trim="track.coverUrl" :disabled="!canWrite" maxlength="1000" placeholder="/images/album-cover.webp">
                  </label>
                </div>

                <button
                  class="music-track-remove"
                  type="button"
                  :disabled="!canWrite"
                  @click="removeMusicTrack(index)"
                >
                  删除
                </button>
              </div>
            </div>

            <div class="profile-upload-row">
              <input
                ref="musicFileInput"
                class="visually-hidden"
                type="file"
                accept=".mp3,audio/mpeg,audio/mp3"
                @change="uploadMusic"
              >
              <button
                type="button"
                :disabled="!canWrite || !mediaConfigured || uploadingMusic"
                @click="chooseMusic"
              >
                {{ uploadingMusic ? '上传中…' : '上传 MP3 并加入列表' }}
              </button>
              <small v-if="!mediaConfigured">对象存储未配置时，仍可填写已有 MP3 链接。</small>
              <small v-else>最大 {{ Math.round(maxAudioSize / 1024 / 1024) }} MB，上传后仍需保存资料。</small>
            </div>
          </div>
        </fieldset>

        <div class="profile-form-actions">
          <button class="primary-action" type="submit" :disabled="!canWrite || saving">
            {{ saving ? '保存中…' : '保存资料' }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>
