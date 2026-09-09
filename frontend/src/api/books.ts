import { apiGet, apiPost } from './request'
import type { Book } from '@/types/models'

export function listBooks() {
  return apiGet<Book[]>('/books')
}

export function selectBook(id: number) {
  return apiPost<void>(`/books/${id}/select`)
}
