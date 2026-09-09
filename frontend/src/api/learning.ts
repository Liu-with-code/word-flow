import { apiGet, apiPost } from './request'
import type {
  ArticleCheckResult,
  FinishDay,
  HintResponse,
  PracticeSentence,
  StepResult,
  TodayPlan,
} from '@/types/models'

export function getTodayPlan() {
  return apiGet<TodayPlan>('/learning/today')
}

export function startLearning() {
  return apiPost<TodayPlan>('/learning/start')
}

/** 协调今日计划：检测词书/每日目标是否变更并同步更新。 */
export function reconcileToday() {
  return apiPost<TodayPlan>('/learning/reconcile')
}

export function getPracticeSentence(wordId: number) {
  return apiGet<PracticeSentence>(`/learning/practice/${wordId}`)
}

/** 流式批改翻译：onChunk 实时返回 AI 增量文本，resolve 时返回最终批改结果。 */
export async function streamTranslate(
  params: {
    wordId: number
    sentence: string
    userTranslation: string
    direction: 'en2zh' | 'zh2en'
  },
  onChunk?: (text: string) => void,
): Promise<StepResult> {
  const token = localStorage.getItem('wordflow_token') || ''
  const response = await fetch('/api/learning/translate-stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(params),
  })
  if (!response.ok || !response.body) {
    throw new Error('AI 流式接口不可用，请刷新后重试')
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  return await new Promise<StepResult>((resolve, reject) => {
    ;(async () => {
      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          let idx = buffer.indexOf('\n\n')
          while (idx >= 0) {
            handleEvent(buffer.slice(0, idx))
            buffer = buffer.slice(idx + 2)
            idx = buffer.indexOf('\n\n')
          }
        }
        if (buffer.trim()) handleEvent(buffer)
      } catch (e) {
        reject(e)
      }
    })()

    function handleEvent(raw: string) {
      const dataLine = raw.split('\n').find((line) => line.startsWith('data:'))
      if (!dataLine) return
      const payload = dataLine.slice(5).trim()
      if (!payload) return
      const message = JSON.parse(payload) as {
        type: 'chunk' | 'done' | 'error'
        payload?: unknown
        message?: string
      }
      if (message.type === 'chunk' && typeof message.payload === 'string') {
        onChunk?.(message.payload)
      } else if (message.type === 'done') {
        resolve(message.payload as StepResult)
      } else if (message.type === 'error') {
        reject(new Error(message.message || 'AI 批改失败'))
      }
    }
  })
}

export function translateHint(wordId: number, direction: 'en2zh' | 'zh2en') {
  return apiPost<HintResponse>('/learning/translate-hint', { wordId, direction })
}

export function checkZh(wordId: number, selectedChinese: string) {
  return apiPost<StepResult>('/learning/check-zh', { wordId, selectedChinese })
}

export function checkEn(wordId: number, selectedWord: string) {
  return apiPost<StepResult>('/learning/check-en', { wordId, selectedWord })
}

export function translateEn(wordId: number, sentence: string, userTranslation: string) {
  return apiPost<StepResult>('/learning/translate-en', { wordId, sentence, userTranslation })
}

export function translateZh(wordId: number, sentence: string, userTranslation: string) {
  return apiPost<StepResult>('/learning/translate-zh', { wordId, sentence, userTranslation })
}

export function completeWord(wordId: number) {
  return apiPost<StepResult>('/learning/complete-word', { wordId })
}

export function finishDay() {
  return apiPost<FinishDay>('/learning/finish-day')
}

export function checkArticle(articleId: number, userTranslation: string) {
  return apiPost<ArticleCheckResult>('/learning/article/check', { articleId, userTranslation })
}
