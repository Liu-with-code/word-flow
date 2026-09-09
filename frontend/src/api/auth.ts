import { apiGet, apiPost, apiPut } from './request'
import type { UserInfo } from '@/types/models'

export interface LoginPayload {
  token: string
  user: UserInfo
}

export function login(username: string, password: string) {
  return apiPost<LoginPayload>('/auth/login', { username, password })
}

export function register(username: string, password: string, nickname: string) {
  return apiPost<LoginPayload>('/auth/register', { username, password, nickname })
}

export function getMe() {
  return apiGet<UserInfo>('/user/me')
}

export function updateProfile(payload: {
  nickname?: string
  avatarUrl?: string
  dailyWordGoal?: number
  dailyReviewGoal?: number
  timezone?: string
  dayBoundaryHour?: number
  nightCutoffHour?: number
}) {
  return apiPut<UserInfo>('/user/profile', payload)
}

export function uploadAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return apiPost<UserInfo>('/user/avatar', form)
}

export function markSetupPromptSeen() {
  return apiPost<UserInfo>('/user/setup-prompt-seen')
}
