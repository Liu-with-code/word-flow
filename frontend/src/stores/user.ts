import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'
import * as booksApi from '@/api/books'
import type { UserInfo } from '@/types/models'
import { detectTimezone } from '@/utils/timezone'

const TOKEN_KEY = 'wordflow_token'
const USER_KEY = 'wordflow_user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<UserInfo | null>(readStoredUser())

  function readStoredUser(): UserInfo | null {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY) || 'null') as UserInfo | null
    } catch {
      return null
    }
  }

  function setSession(newToken: string, newUser: UserInfo) {
    token.value = newToken
    user.value = newUser
    localStorage.setItem(TOKEN_KEY, newToken)
    localStorage.setItem(USER_KEY, JSON.stringify(newUser))
  }

  async function login(username: string, password: string) {
    const result = await authApi.login(username, password)
    setSession(result.token, result.user)
    await syncTimezone()
  }

  async function register(username: string, password: string, nickname: string) {
    const result = await authApi.register(username, password, nickname)
    setSession(result.token, result.user)
    await syncTimezone()
  }

  async function fetchMe() {
    user.value = await authApi.getMe()
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  async function updateProfile(payload: {
    nickname?: string
    avatarUrl?: string
    dailyWordGoal?: number
    dailyReviewGoal?: number
    timezone?: string
    dayBoundaryHour?: number
    nightCutoffHour?: number
  }) {
    user.value = await authApi.updateProfile(payload)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  /** 首次登录/注册后同步用户时区，后续设备时区变化也会被纠正。 */
  async function syncTimezone() {
    const detected = detectTimezone()
    if (user.value && user.value.timezone !== detected) {
      await updateProfile({ timezone: detected })
    }
  }

  async function selectBook(bookId: number) {
    await booksApi.selectBook(bookId)
    await fetchMe()
  }

  async function uploadAvatar(file: File) {
    user.value = await authApi.uploadAvatar(file)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  /** 记录设置弹窗已处理（服务端持久化），避免每次打开重复弹出。 */
  async function markSetupPromptSeen() {
    user.value = await authApi.markSetupPromptSeen()
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    user,
    login,
    register,
    fetchMe,
    updateProfile,
    selectBook,
    syncTimezone,
    uploadAvatar,
    markSetupPromptSeen,
    logout,
  }
})
