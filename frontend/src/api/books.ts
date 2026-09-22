import { apiGet, apiPost } from './request';
import type { Book } from '@/types/models';

export function listBooks() {
  return apiGet<Book[]>('/books');
}

export function selectBook(id: number) {
  // 后端返回 data 为 null，用 unknown 表达「无业务返回值」
  return apiPost<unknown>(`/books/${id}/select`);
}

/** 重新背诵指定词书：清空本书进度使其重新可学，其他词书进度保留 */
export function restartBook(id: number) {
  return apiPost<Book[]>(`/books/${id}/restart`);
}
