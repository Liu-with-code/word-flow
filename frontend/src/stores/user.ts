import { defineStore } from 'pinia';
import { ref } from 'vue';
import * as authApi from '@/api/auth';
import * as booksApi from '@/api/books';
import type { UserInfo } from '@/types/models';
import {
  clearSession,
  getStoredUser,
  getToken,
  setStoredUser,
  setToken as persistToken,
} from '@/utils/auth';
import { detectTimezone } from '@/utils/timezone';

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken());
  const user = ref<UserInfo | null>(getStoredUser());

  function setSession(newToken: string, newUser: UserInfo) {
    token.value = newToken;
    user.value = newUser;
    persistToken(newToken);
    setStoredUser(newUser);
  }

  function cacheUser(newUser: UserInfo) {
    user.value = newUser;
    setStoredUser(newUser);
  }

  async function login(username: string, password: string) {
    const result = await authApi.login(username, password);
    setSession(result.token, result.user);
    await syncTimezone();
  }

  async function register(username: string, password: string, nickname: string) {
    const result = await authApi.register(username, password, nickname);
    setSession(result.token, result.user);
    await syncTimezone();
  }

  async function fetchMe() {
    cacheUser(await authApi.getMe());
  }

  async function updateProfile(payload: {
    nickname?: string;
    avatarUrl?: string;
    dailyWordGoal?: number;
    dailyReviewGoal?: number;
    timezone?: string;
    dayBoundaryHour?: number;
    nightCutoffHour?: number;
  }) {
    cacheUser(await authApi.updateProfile(payload));
  }

  /** 首次登录/注册后同步用户时区，后续设备时区变化也会被纠正。 */
  async function syncTimezone() {
    const detected = detectTimezone();
    if (user.value && user.value.timezone !== detected) {
      await updateProfile({ timezone: detected });
    }
  }

  async function selectBook(bookId: number) {
    await booksApi.selectBook(bookId);
    await fetchMe();
  }

  async function uploadAvatar(file: File) {
    cacheUser(await authApi.uploadAvatar(file));
  }

  /** 记录设置弹窗已处理（服务端持久化），避免每次打开重复弹出。 */
  async function markSetupPromptSeen() {
    cacheUser(await authApi.markSetupPromptSeen());
  }

  function logout() {
    token.value = '';
    user.value = null;
    clearSession();
  }

  return {
    token,
    user,
    login,
    register,
    fetchMe,
    updateProfile,
    selectBook,
    syncTimezone,
    uploadAvatar,
    markSetupPromptSeen,
    logout,
  };
});
