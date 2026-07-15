<script setup lang="ts">
import type { MusicTrack, RepeatMode } from '~/types/music'
import { formatDuration, getNextIndex, getPreviousIndex } from '~/utils/musicPlayback'

interface PlaylistResponse {
  items: MusicTrack[]
}

const audio = ref<HTMLAudioElement>()
const tracks = ref<MusicTrack[]>([])
const currentIndex = ref(0)
const currentTime = ref(0)
const duration = ref(0)
const volume = ref(0.7)
const isPlaying = ref(false)
const isMuted = ref(false)
const isLoading = ref(true)
const isExpanded = ref(false)
const showPlaylist = ref(false)
const repeat = ref<RepeatMode>('all')
const errorMessage = ref('')

let collapseTimer: number | undefined
let retriedStreamFor: string | undefined

const currentTrack = computed(() => tracks.value[currentIndex.value] ?? null)
const repeatLabel = computed(() => ({ off: '不循环', one: '单曲循环', all: '列表循环' })[repeat.value])

const clearCollapseTimer = () => {
  if (collapseTimer !== undefined) window.clearTimeout(collapseTimer)
  collapseTimer = undefined
}

const expand = () => {
  clearCollapseTimer()
  isExpanded.value = true
}

const scheduleCollapse = () => {
  clearCollapseTimer()
  collapseTimer = window.setTimeout(() => {
    isExpanded.value = false
    showPlaylist.value = false
  }, 420)
}

const showError = (message: string) => {
  errorMessage.value = message
  isLoading.value = false
}

const requestStream = async (track: MusicTrack) => {
  const response = await $fetch<{ url: string }>(`/music/playlist/${encodeURIComponent(track.id)}/stream`)
  if (!response.url) throw new Error('empty stream')
  return response.url
}

const loadTrack = async (index: number, autoplay: boolean, freshRequest = true) => {
  const track = tracks.value[index]
  const player = audio.value
  if (!track || !player) return

  currentIndex.value = index
  errorMessage.value = ''
  isLoading.value = true

  let streamUrl: string

  try {
    streamUrl = await requestStream(track)
  } catch {
    const next = getNextIndex(index, tracks.value.length, 'all')
    if (freshRequest && next !== null && next !== index) {
      await loadTrack(next, autoplay, false)
      return
    }

    showError('当前歌曲暂时无法播放')
    return
  }

  try {
    player.src = streamUrl
    player.load()
    currentTime.value = 0
    duration.value = 0
    if (freshRequest) retriedStreamFor = undefined
    if (autoplay) await player.play()
  } catch {
    showError('当前歌曲暂时无法播放')
  } finally {
    isLoading.value = false
  }
}

const togglePlayback = async () => {
  const player = audio.value
  if (!player || !currentTrack.value) return

  if (!player.paused) {
    player.pause()
    isPlaying.value = false
    return
  }

  if (!player.src) {
    await loadTrack(currentIndex.value, true)
    return
  }

  try {
    await player.play()
  } catch {
    showError('浏览器阻止了音频播放')
  }
}

const selectTrack = async (index: number) => {
  await loadTrack(index, true)
}

const playNext = async () => {
  const next = getNextIndex(currentIndex.value, tracks.value.length, repeat.value)
  if (next !== null) await loadTrack(next, true)
  else isPlaying.value = false
}

const playPrevious = async () => {
  const previous = getPreviousIndex(currentIndex.value, tracks.value.length)
  if (previous !== null) await loadTrack(previous, true)
}

const cycleRepeat = () => {
  repeat.value = repeat.value === 'off' ? 'one' : repeat.value === 'one' ? 'all' : 'off'
}

const seek = (event: Event) => {
  const nextTime = Number((event.target as HTMLInputElement).value)
  if (!audio.value || !Number.isFinite(nextTime)) return
  audio.value.currentTime = nextTime
  currentTime.value = nextTime
}

const updateVolume = () => {
  if (!audio.value) return
  audio.value.volume = volume.value
  audio.value.muted = false
  isMuted.value = false
}

const toggleMute = () => {
  if (!audio.value) return
  audio.value.muted = !audio.value.muted
  isMuted.value = audio.value.muted
}

const handleAudioError = () => {
  const track = currentTrack.value
  if (!track || retriedStreamFor === track.id) {
    showError('音频资源无法加载')
    return
  }

  retriedStreamFor = track.id
  void loadTrack(currentIndex.value, isPlaying.value, false)
}

onMounted(async () => {
  try {
    const response = await $fetch<PlaylistResponse>('/music/playlist')
    tracks.value = response.items
    if (!tracks.value.length) showError('歌单中没有可播放歌曲')
  } catch {
    showError('QQ 音乐歌单加载失败')
  } finally {
    isLoading.value = false
  }
})

onUnmounted(() => clearCollapseTimer())
</script>

<template>
  <section
    class="music-player"
    :class="{ 'music-player--expanded': isExpanded }"
    aria-label="音乐播放器"
    @pointerenter="expand"
    @pointerleave="scheduleCollapse"
    @focusin="expand"
    @focusout="scheduleCollapse"
  >
    <button
      class="music-player__edge"
      type="button"
      :aria-label="isExpanded ? '收起音乐播放器' : '展开音乐播放器'"
      @click="isExpanded = !isExpanded"
    >
      <span aria-hidden="true">♫</span>
    </button>

    <div class="music-player__panel">
      <div class="music-player__now-playing">
        <img class="music-player__cover" :src="currentTrack?.cover || '/images/wineclouds-avatar.png'" alt="">
        <div class="music-player__track-copy">
          <strong>{{ currentTrack?.title || '正在准备歌单' }}</strong>
          <small>{{ currentTrack?.artist || 'QQ 音乐' }}</small>
        </div>
        <button class="music-player__icon-button" type="button" :aria-label="isPlaying ? '暂停' : '播放'" @click="togglePlayback">
          {{ isPlaying ? 'Ⅱ' : '▶' }}
        </button>
      </div>

      <label class="music-player__progress" aria-label="播放进度">
        <input type="range" min="0" :max="duration || 0" :value="currentTime" :disabled="!duration" @input="seek">
        <span>{{ formatDuration(currentTime) }} / {{ formatDuration(duration) }}</span>
      </label>

      <div class="music-player__controls">
        <button type="button" aria-label="上一首" @click="playPrevious">↶</button>
        <button type="button" :aria-label="isMuted ? '取消静音' : '静音'" @click="toggleMute">{{ isMuted ? '🔇' : '🔊' }}</button>
        <label class="music-player__volume" aria-label="音量"><input v-model.number="volume" type="range" min="0" max="1" step="0.01" @input="updateVolume"></label>
        <button type="button" :aria-label="repeatLabel" :title="repeatLabel" @click="cycleRepeat">↻</button>
        <button type="button" aria-label="下一首" @click="playNext">↷</button>
        <button type="button" :aria-expanded="showPlaylist" aria-label="播放列表" @click="showPlaylist = !showPlaylist">☷</button>
      </div>

      <p v-if="isLoading" class="music-player__status">正在加载…</p>
      <p v-else-if="errorMessage" class="music-player__status music-player__status--error">{{ errorMessage }}</p>

      <ol v-if="showPlaylist" class="music-player__playlist" aria-label="播放列表">
        <li v-for="(track, index) in tracks" :key="track.id" :class="{ active: index === currentIndex }">
          <button type="button" @click="selectTrack(index)">
            <span>{{ String(index + 1).padStart(2, '0') }}</span>
            <b>{{ track.title }}</b>
            <small>{{ track.artist }}</small>
          </button>
        </li>
      </ol>
    </div>

    <audio
      ref="audio"
      preload="metadata"
      @play="isPlaying = true"
      @playing="isPlaying = true"
      @pause="isPlaying = false"
      @timeupdate="currentTime = audio?.currentTime || 0"
      @loadedmetadata="duration = audio?.duration || 0"
      @ended="playNext"
      @error="handleAudioError"
    />
  </section>
</template>
