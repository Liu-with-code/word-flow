import { apiGet, apiPost } from './request';
import type {
  ArticleCheckResult,
  NightPrompt,
  ReviewOverview,
  ReviewStart,
  WarmupData,
  WarmupMarkResult,
} from '@/types/models';

export function getReviewOverview() {
  return apiGet<ReviewOverview>('/review/overview');
}

export function startReview() {
  return apiPost<ReviewStart>('/review/start');
}

export function checkReview(articleId: number, userTranslation: string) {
  return apiPost<ArticleCheckResult>('/review/check', { articleId, userTranslation });
}

export function getNightPrompt() {
  return apiGet<NightPrompt>('/review/night-prompt');
}

export function answerNightPrompt(apply: boolean) {
  // 后端返回 data 为 null，用 unknown 表达「无业务返回值」
  return apiPost<unknown>('/review/night-prompt', { apply });
}

/** 复习热身：到期单词快速自测 */
export function getWarmup() {
  return apiGet<WarmupData>('/review/warmup');
}

/** 热身作答：记得=true 推进复习阶段，false 留待短文复习 */
export function markWarmup(wordId: number, remembered: boolean) {
  return apiPost<WarmupMarkResult>('/review/warmup', { wordId, remembered });
}
