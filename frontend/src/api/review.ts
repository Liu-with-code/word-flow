import { apiGet, apiPost } from './request'
import type { ArticleCheckResult, NightPrompt, ReviewOverview, ReviewStart } from '@/types/models'

export function getReviewOverview() {
  return apiGet<ReviewOverview>('/review/overview')
}

export function startReview() {
  return apiPost<ReviewStart>('/review/start')
}

export function checkReview(articleId: number, userTranslation: string) {
  return apiPost<ArticleCheckResult>('/review/check', { articleId, userTranslation })
}

export function getNightPrompt() {
  return apiGet<NightPrompt>('/review/night-prompt')
}

export function answerNightPrompt(apply: boolean) {
  return apiPost<void>('/review/night-prompt', { apply })
}
