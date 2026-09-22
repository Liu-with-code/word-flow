import type { UserInfo } from '@/types/models';

/**
 * 本地会话存储统一收口：键名与读写集中管理，并对存储异常做兜底。
 */

const TOKEN_KEY = 'wordflow_token';
const USER_KEY = 'wordflow_user';

/** 读取登录令牌 */
export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || '';
}

/** 写入登录令牌 */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

/** 读取缓存的用户信息（解析失败返回 null） */
export function getStoredUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as UserInfo) : null;
  } catch {
    return null;
  }
}

/** 缓存用户信息 */
export function setStoredUser(user: UserInfo): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

/** 清空登录态 */
export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

/** 是否已登录 */
export function hasToken(): boolean {
  return Boolean(getToken());
}
