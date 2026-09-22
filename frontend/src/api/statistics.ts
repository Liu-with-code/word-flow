import { apiGet } from './request';
import type {
  DashboardStats,
  PageResult,
  RecentRecord,
  ReviewForecast,
  StageDistribution,
  WeakWord,
  WeeklyPoint,
} from '@/types/models';

export function getDashboard() {
  return apiGet<DashboardStats>('/stats/dashboard');
}

export function getWeekly() {
  return apiGet<WeeklyPoint[]>('/stats/weekly');
}

export function getRecent(limit = 8) {
  return apiGet<RecentRecord[]>('/stats/recent', { limit });
}

/** 记忆阶段分布（艾宾浩斯各阶段单词数量） */
export function getStageDistribution() {
  return apiGet<StageDistribution>('/stats/stage-distribution');
}

/** 未来复习量预测（默认 14 天） */
export function getReviewForecast(days = 14) {
  return apiGet<ReviewForecast>('/stats/review-forecast', { days });
}

/** 薄弱单词概览 */
export function getWeakWords(limit = 6) {
  return apiGet<WeakWord[]>('/stats/weak-words', { onlyWrong: true, limit });
}

/** 错题本分页查询 */
export function getWeakWordPage(page = 1, size = 10) {
  return apiGet<PageResult<WeakWord>>('/stats/weak-words/page', { page, size });
}
