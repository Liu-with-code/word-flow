import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/splash',
      name: 'splash',
      component: () => import('@/views/SplashView.vue'),
      meta: { title: 'WordFlow 词流' },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { title: '注册' },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'learn',
          name: 'learn',
          component: () => import('@/views/LearnView.vue'),
          meta: { title: '今日学习' },
        },
        {
          path: 'review',
          name: 'review',
          component: () => import('@/views/ReviewView.vue'),
          meta: { title: '复习' },
        },
        {
          path: 'words',
          name: 'words',
          component: () => import('@/views/WordsView.vue'),
          meta: { title: '词书' },
        },
        {
          path: 'stats',
          name: 'stats',
          component: () => import('@/views/StatsView.vue'),
          meta: { title: '统计' },
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/views/SettingsView.vue'),
          meta: { title: '设置' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  document.title = `${String(to.meta.title || '')} · WordFlow 词流`

  // 已登录时：开屏页与登录/注册页都不再展示
  if (userStore.token) {
    if (to.name === 'splash' || to.name === 'login' || to.name === 'register') {
      return { name: 'home' }
    }
    return true
  }

  // 未登录：每次冷启动先展示一次开屏
  if (!sessionStorage.getItem('wordflow_splash_seen')) {
    sessionStorage.setItem('wordflow_splash_seen', '1')
    if (to.name !== 'splash') {
      return { name: 'splash', query: { next: to.fullPath } }
    }
    return true
  }

  if (to.meta.requiresAuth) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
