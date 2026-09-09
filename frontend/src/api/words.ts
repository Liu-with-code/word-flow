import { apiGet } from './request'
import type { PageResult, Word } from '@/types/models'

export function queryWords(params: {
  keyword?: string
  bookId?: number
  level?: string
  page?: number
  size?: number
}) {
  return apiGet<PageResult<Word>>('/words', params)
}
