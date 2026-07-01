import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { api } from '../services/api'

export type AdminUser = {
  id: string
  username: string
  nickname: string
  email?: string
  role: 'ADMIN' | 'DEMO'
}

type AuthPayload = {
  accessToken: string
  expiresAt: string
  user: AdminUser
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AdminUser | null>(null)
  const initialized = ref(false)
  const authenticated = computed(() => user.value !== null)

  const apply = (payload: AuthPayload) => {
    api.setAccessToken(payload.accessToken)
    user.value = payload.user
  }

  const login = async (username: string, password: string) => {
    const payload = await api.post<AuthPayload>('/auth/login', { username, password })
    apply(payload)
  }

  const restore = async () => {
    if (initialized.value) return authenticated.value
    try {
      if (!await api.refresh()) return false
      user.value = await api.get<AdminUser>('/auth/me')
      return true
    } finally {
      initialized.value = true
    }
  }

  const logout = async () => {
    await api.logout()
    user.value = null
  }

  return { user, initialized, authenticated, login, restore, logout }
})
