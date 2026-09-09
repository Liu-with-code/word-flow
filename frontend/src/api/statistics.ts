import { apiGet } from './request'
import type { DashboardStats, RecentRecord, WeeklyPoint } from '@/types/models'

export function getDashboard() {
  return apiGet<DashboardStats>('/stats/dashboard')
}

export function getWeekly() {
  return apiGet<WeeklyPoint[]>('/stats/weekly')
}

export function getRecent(limit = 8) {
  return apiGet<RecentRecord[]>('/stats/recent', { limit })
}

